package com.kara.kara_general_api.application.service.order

import com.kara.kara_general_api.domain.model.booking.Booking
import com.kara.kara_general_api.domain.model.booking.BookingId
import com.kara.kara_general_api.domain.model.booking.BookingStatus
import com.kara.kara_general_api.domain.model.product.Product
import com.kara.kara_general_api.domain.model.product.ProductId
import com.kara.kara_general_api.domain.model.room.Currency
import com.kara.kara_general_api.domain.model.room.RoomId
import com.kara.kara_general_api.domain.model.stock.RoomStockItem
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.port.input.order.PlaceOrderCommand
import com.kara.kara_general_api.domain.port.input.order.PlaceOrderResult
import com.kara.kara_general_api.domain.port.output.BookingRepository
import com.kara.kara_general_api.domain.port.output.OrderPlacedEventPublisher
import com.kara.kara_general_api.domain.port.output.OrderRepository
import com.kara.kara_general_api.domain.port.output.PaymentMethodPort
import com.kara.kara_general_api.domain.port.output.ProductRepository
import com.kara.kara_general_api.domain.port.output.RoomStockRepository
import com.kara.kara_general_api.domain.port.output.ServerShiftRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertInstanceOf
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals

class PlaceOrderServiceTest {

    private val bookingRepository = mockk<BookingRepository>(relaxed = true)
    private val productRepository = mockk<ProductRepository>(relaxed = true)
    private val roomStockRepository = mockk<RoomStockRepository>(relaxed = true)
    private val orderRepository = mockk<OrderRepository>(relaxed = true)
    private val paymentMethodPort = mockk<PaymentMethodPort>(relaxed = true)
    private val serverShiftRepository = mockk<ServerShiftRepository>(relaxed = true)
    private val orderPlacedEventPublisher = mockk<OrderPlacedEventPublisher>(relaxed = true)

    private val sut =
        PlaceOrderService(
            bookingRepository,
            productRepository,
            roomStockRepository,
            orderRepository,
            paymentMethodPort,
            serverShiftRepository,
            orderPlacedEventPublisher,
        )

    private val roomId = RoomId(UUID.randomUUID())
    private val userId = UserId(UUID.randomUUID())
    private val productId = ProductId(UUID.randomUUID())
    private val bookingId = BookingId(UUID.randomUUID())

    private fun activeBooking(
        status: BookingStatus = BookingStatus.CONFIRMED,
        startAt: Instant = Instant.now().minusSeconds(600),
        endAt: Instant = Instant.now().plusSeconds(600),
        owner: UserId = userId,
    ) = Booking(
        id = bookingId,
        roomId = roomId,
        userId = owner,
        startAt = startAt,
        endAt = endAt,
        numberOfPeople = 8,
        selectedOptionIds = emptyList(),
        totalPrice = BigDecimal("435.00"),
        currency = Currency.EUR,
        status = status,
        createdAt = Instant.now().minusSeconds(3600),
        expiresAt = Instant.now().minusSeconds(3000),
    )

    private fun product() =
        Product(
            id = productId,
            name = "Coca-Cola 33cl",
            description = null,
            price = BigDecimal("2.50"),
            currency = Currency.EUR,
        )

    private fun command(quantity: Int = 2) =
        PlaceOrderCommand(bookingId = bookingId, productId = productId, quantity = quantity, currentUserId = userId)

    @Test
    fun `places the order, decrements stock and saves the order when payment method is registered`() {
        every { bookingRepository.findById(bookingId) } returns activeBooking()
        every { productRepository.findById(productId) } returns product()
        every { roomStockRepository.findQuantity(roomId, productId) } returns 10
        every { paymentMethodPort.hasRegisteredPaymentMethod(userId) } returns true
        val savedSlot = slot<com.kara.kara_general_api.domain.model.order.Order>()
        every { orderRepository.save(capture(savedSlot)) } answers { savedSlot.captured }
        val serverId = UserId(UUID.randomUUID())
        every { serverShiftRepository.findServerIdsAssignedTo(roomId, any(), any()) } returns setOf(serverId)

        val result = sut.placeOrder(command(quantity = 2))

        val success = assertInstanceOf<PlaceOrderResult.Success>(result)
        assertEquals(BigDecimal("2.50"), success.order.unitPrice)
        assertEquals(BigDecimal("5.00"), success.order.totalPrice)
        assertEquals(2, success.order.quantity)
        verify(exactly = 1) { roomStockRepository.upsert(RoomStockItem(roomId, productId, 8)) }
        verify(exactly = 1) { orderRepository.save(any()) }
        verify(exactly = 1) { orderPlacedEventPublisher.publishOrderPlaced(serverId, any()) }
    }

    @Test
    fun `notifies every assigned server when the order is placed`() {
        every { bookingRepository.findById(bookingId) } returns activeBooking()
        every { productRepository.findById(productId) } returns product()
        every { roomStockRepository.findQuantity(roomId, productId) } returns 10
        every { paymentMethodPort.hasRegisteredPaymentMethod(userId) } returns true
        val savedSlot = slot<com.kara.kara_general_api.domain.model.order.Order>()
        every { orderRepository.save(capture(savedSlot)) } answers { savedSlot.captured }
        val serverA = UserId(UUID.randomUUID())
        val serverB = UserId(UUID.randomUUID())
        every { serverShiftRepository.findServerIdsAssignedTo(roomId, any(), any()) } returns setOf(serverA, serverB)

        sut.placeOrder(command(quantity = 1))

        verify(exactly = 1) { orderPlacedEventPublisher.publishOrderPlaced(serverA, any()) }
        verify(exactly = 1) { orderPlacedEventPublisher.publishOrderPlaced(serverB, any()) }
    }

    @Test
    fun `returns BookingNotFound when the booking does not exist`() {
        every { bookingRepository.findById(bookingId) } returns null

        val result = sut.placeOrder(command())

        assertEquals(PlaceOrderResult.BookingNotFound, result)
        verify(exactly = 0) { roomStockRepository.upsert(any()) }
        verify(exactly = 0) { orderRepository.save(any()) }
    }

    @Test
    fun `returns NotOwner when the booking belongs to another user`() {
        every { bookingRepository.findById(bookingId) } returns activeBooking(owner = UserId(UUID.randomUUID()))

        val result = sut.placeOrder(command())

        assertEquals(PlaceOrderResult.NotOwner, result)
        verify(exactly = 0) { orderRepository.save(any()) }
    }

    @Test
    fun `returns BookingNotActive when the booking is confirmed but outside the time window`() {
        every { bookingRepository.findById(bookingId) } returns
            activeBooking(
                startAt = Instant.now().plusSeconds(600),
                endAt = Instant.now().plusSeconds(1200),
            )

        val result = sut.placeOrder(command())

        assertEquals(PlaceOrderResult.BookingNotActive, result)
        verify(exactly = 0) { orderRepository.save(any()) }
    }

    @Test
    fun `returns BookingNotActive when the booking status is not confirmed`() {
        every { bookingRepository.findById(bookingId) } returns activeBooking(status = BookingStatus.PENDING)

        val result = sut.placeOrder(command())

        assertEquals(PlaceOrderResult.BookingNotActive, result)
        verify(exactly = 0) { orderRepository.save(any()) }
    }

    @Test
    fun `returns ProductNotFound when the product does not exist`() {
        every { bookingRepository.findById(bookingId) } returns activeBooking()
        every { productRepository.findById(productId) } returns null

        val result = sut.placeOrder(command())

        assertEquals(PlaceOrderResult.ProductNotFound, result)
        verify(exactly = 0) { orderRepository.save(any()) }
    }

    @Test
    fun `returns InsufficientStock when the room stock is below the requested quantity`() {
        every { bookingRepository.findById(bookingId) } returns activeBooking()
        every { productRepository.findById(productId) } returns product()
        every { roomStockRepository.findQuantity(roomId, productId) } returns 1

        val result = sut.placeOrder(command(quantity = 5))

        assertEquals(PlaceOrderResult.InsufficientStock, result)
        verify(exactly = 0) { roomStockRepository.upsert(any()) }
        verify(exactly = 0) { orderRepository.save(any()) }
    }

    @Test
    fun `returns InsufficientStock when the product is not in the room stock`() {
        every { bookingRepository.findById(bookingId) } returns activeBooking()
        every { productRepository.findById(productId) } returns product()
        every { roomStockRepository.findQuantity(roomId, productId) } returns null

        val result = sut.placeOrder(command(quantity = 1))

        assertEquals(PlaceOrderResult.InsufficientStock, result)
        verify(exactly = 0) { orderRepository.save(any()) }
    }

    @Test
    fun `returns PaymentMethodRequired and persists nothing when no payment method is registered`() {
        every { bookingRepository.findById(bookingId) } returns activeBooking()
        every { productRepository.findById(productId) } returns product()
        every { roomStockRepository.findQuantity(roomId, productId) } returns 10
        every { paymentMethodPort.hasRegisteredPaymentMethod(userId) } returns false

        val result = sut.placeOrder(command(quantity = 2))

        assertEquals(PlaceOrderResult.PaymentMethodRequired, result)
        verify(exactly = 0) { roomStockRepository.upsert(any()) }
        verify(exactly = 0) { orderRepository.save(any()) }
        verify(exactly = 0) { orderPlacedEventPublisher.publishOrderPlaced(any(), any()) }
    }
}

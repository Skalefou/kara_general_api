package com.kara.kara_general_api.infrastructure.adapter.output.persistence.pool

import com.kara.kara_general_api.domain.model.payment.PoolId
import com.kara.kara_general_api.domain.model.payment.PoolShare
import com.kara.kara_general_api.domain.model.payment.PoolShareId
import com.kara.kara_general_api.domain.model.payment.PoolShareStatus
import com.kara.kara_general_api.domain.model.user.UserId
import com.kara.kara_general_api.domain.model.user.vo.Email
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Component
import java.sql.ResultSet
import java.util.UUID

@Component
class PoolShareRowMapper : RowMapper<PoolShare> {
    override fun mapRow(rs: ResultSet, rowNum: Int): PoolShare =
        PoolShare(
            id = PoolShareId(rs.getObject("id", UUID::class.java)),
            poolId = PoolId(rs.getObject("pool_id", UUID::class.java)),
            participantName = rs.getString("participant_name"),
            email = rs.getString("email")?.let { Email(it) },
            amount = rs.getBigDecimal("amount"),
            status = PoolShareStatus.valueOf(rs.getString("status")),
            stripePaymentIntentId = rs.getString("stripe_payment_intent_id"),
            uniqueLinkToken = rs.getString("unique_link_token"),
            payerUserId = rs.getObject("payer_user_id", UUID::class.java)?.let { UserId(it) },
            isCreatorShare = rs.getBoolean("is_creator_share"),
        )
}

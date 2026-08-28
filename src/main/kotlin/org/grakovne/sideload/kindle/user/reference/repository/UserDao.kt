package org.grakovne.sideload.kindle.user.reference.repository

import org.grakovne.sideload.kindle.generated.tables.User.Companion.USER
import org.grakovne.sideload.kindle.generated.tables.records.UserRecord
import org.grakovne.sideload.kindle.user.reference.domain.Type
import org.grakovne.sideload.kindle.user.reference.domain.User
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

@Repository
class UserDao(
    private val dsl: DSLContext
) {

    fun save(user: User): User {
        dsl.insertInto(USER)
            .set(USER.ID, user.id)
            .set(USER.LANGUAGE, user.language)
            .set(USER.TYPE, user.type.name)
            .set(USER.LAST_ACTIVITY_TIMESTAMP, toDb(user.lastActivityTimestamp))
            .onConflict(USER.ID)
            .doUpdate()
            .set(USER.LANGUAGE, user.language)
            .set(USER.TYPE, user.type.name)
            .set(USER.LAST_ACTIVITY_TIMESTAMP, toDb(user.lastActivityTimestamp))
            .execute()
        return user
    }

    fun findById(id: String): User? =
        dsl.selectFrom(USER)
            .where(USER.ID.eq(id))
            .fetchOne()
            ?.let { it.toDomain() }

    fun findByLastActivityTimestampGreaterThanAndLastActivityTimestampLessThan(
        from: Instant,
        to: Instant
    ): List<User> {
        return dsl.selectFrom(USER)
            .where(USER.LAST_ACTIVITY_TIMESTAMP.gt(toDb(from)))
            .and(USER.LAST_ACTIVITY_TIMESTAMP.lt(toDb(to)))
            .fetch()
            .map { it.toDomain() }
    }

    fun findByType(type: Type): List<User> {
        return dsl.selectFrom(USER)
            .where(USER.TYPE.eq(type.name))
            .fetch()
            .map { it.toDomain() }
    }

    fun touchLastActivity(id: String, timestamp: Instant): Int {
        return dsl.update(USER)
            .set(USER.LAST_ACTIVITY_TIMESTAMP, toDb(timestamp))
            .where(USER.ID.eq(id))
            .execute()
    }

    fun touchLastActivity(ids: Collection<String>, timestamp: Instant): Int {
        if (ids.isEmpty()) {
            return 0
        }

        return dsl.update(USER)
            .set(USER.LAST_ACTIVITY_TIMESTAMP, toDb(timestamp))
            .where(USER.ID.`in`(ids))
            .execute()
    }

    fun saveAll(users: List<User>) = users.forEach { save(it) }

    fun findAll(): List<User> =
        dsl.selectFrom(USER).fetch().map { it.toDomain() }

    fun count(): Int = dsl.fetchCount(USER)

    fun deleteAll() = dsl.deleteFrom(USER).execute()

    private fun toDb(instant: Instant?): LocalDateTime? =
        instant?.let { LocalDateTime.ofInstant(it, ZoneOffset.UTC) }

    private fun fromDb(localDateTime: LocalDateTime?): Instant? =
        localDateTime?.let { it.toInstant(ZoneOffset.UTC) }

    private fun UserRecord.toDomain(): User =
        User(
            id = id!!,
            language = language!!,
            type = Type.valueOf(type!!),
            lastActivityTimestamp = fromDb(lastActivityTimestamp)
        )
}

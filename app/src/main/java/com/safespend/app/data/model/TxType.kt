package com.safespend.app.data.model

/** Money coming in, or money going out. Everything else is derived from this. */
enum class TxType { INCOME, EXPENSE }

/** Where a transaction came from — used to show the "auto-captured" badge. */
enum class TxSource { MANUAL, SMS_IMPORT }

package com.mobilebytesensei.featurerequest.model

enum class TicketType(
    val value: String,
    val label: String,
    val emoji: String,
    val isPrivate: Boolean,
) {
    FEATURE_REQUEST("feature_request", "Feature Request", "\uD83D\uDCA1", false),
    BUG_REPORT("bug_report", "Bug Report", "\uD83D\uDC1B", false),
    CONTACT_SUPPORT("contact_support", "Contact Support", "\uD83D\uDD12", true),
}

enum class TicketCategory(
    val value: String,
    val label: String,
    val emoji: String,
    val applicableTo: List<TicketType>,
) {
    NEW_FEATURE("new_feature", "New Feature", "\u2728", listOf(TicketType.FEATURE_REQUEST)),
    UI_DESIGN("ui_design", "UI/Design", "\uD83C\uDFA8", listOf(TicketType.FEATURE_REQUEST)),
    PERFORMANCE("performance", "Performance", "\u26A1", listOf(TicketType.FEATURE_REQUEST, TicketType.BUG_REPORT)),
    DOWNLOAD("download", "Download", "\uD83D\uDCE5", listOf(TicketType.FEATURE_REQUEST, TicketType.BUG_REPORT)),
    CRASH("crash", "Crash", "\uD83D\uDCA5", listOf(TicketType.BUG_REPORT)),
    UI_BUG("ui_bug", "UI Bug", "\uD83D\uDDBC\uFE0F", listOf(TicketType.BUG_REPORT)),
    SUBSCRIPTION("subscription", "Subscription", "\uD83D\uDCB3", listOf(TicketType.CONTACT_SUPPORT)),
    ACCOUNT("account", "Account", "\uD83D\uDC64", listOf(TicketType.CONTACT_SUPPORT)),
    BILLING("billing", "Billing", "\uD83E\uDDFE", listOf(TicketType.CONTACT_SUPPORT)),
    GENERAL("general", "General", "\uD83D\uDCDD", TicketType.entries.toList()),
}

enum class TicketStatus(val value: String, val label: String) {
    PENDING("pending", "Pending"),
    PLANNED("planned", "Planned"),
    IN_REVIEW("in_review", "In Review"),
    IN_PROGRESS("in_progress", "In Progress"),
    RESOLVED("resolved", "Resolved"),
    COMPLETED("completed", "Completed"),
    CLOSED("closed", "Closed"),
}

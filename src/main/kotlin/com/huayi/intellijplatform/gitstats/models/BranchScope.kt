package com.huayi.intellijplatform.gitstats.models

data class BranchInfo(
    val currentBranch: String? = null,
    val localBranches: List<String> = emptyList(),
    val remoteBranches: List<String> = emptyList()
) {
    val selectableBranches: List<BranchRef>
        get() = localBranches.map { BranchRef.local(it) } + remoteBranches.map { BranchRef.remote(it) }
}

data class BranchRef(
    val name: String,
    val refName: String,
    val type: BranchRefType
) {
    override fun toString(): String = name

    companion object {
        fun local(name: String): BranchRef = BranchRef(name, "refs/heads/$name", BranchRefType.LOCAL)
        fun remote(name: String): BranchRef = BranchRef(name, "refs/remotes/$name", BranchRefType.REMOTE)
    }
}

enum class BranchRefType {
    LOCAL,
    REMOTE
}

enum class BranchScopeMode(val labelKey: String) {
    CURRENT_BRANCH("branchScopeCurrent"),
    HEAD("branchScopeHead"),
    ALL_LOCAL_BRANCHES("branchScopeAllLocal"),
    SELECTED_BRANCH("branchScopeSelected"),
    SELECTED_BRANCHES("branchScopeSelected"),
    CUSTOM_REVISION_RANGE("branchScopeCustom")
}

sealed class BranchScope {
    abstract val mode: BranchScopeMode

    object CurrentBranch : BranchScope() {
        override val mode = BranchScopeMode.CURRENT_BRANCH
    }

    object Head : BranchScope() {
        override val mode = BranchScopeMode.HEAD
    }

    object AllLocalBranches : BranchScope() {
        override val mode = BranchScopeMode.ALL_LOCAL_BRANCHES
    }

    data class SelectedBranch(val branchName: String, val refName: String) : BranchScope() {
        override val mode = BranchScopeMode.SELECTED_BRANCH
    }

    data class SelectedBranches(val branches: List<SelectedBranch>) : BranchScope() {
        override val mode = BranchScopeMode.SELECTED_BRANCHES
    }

    data class CustomRevisionRange(val revisionRange: String) : BranchScope() {
        override val mode = BranchScopeMode.CUSTOM_REVISION_RANGE
    }

    companion object {
        fun selectedBranch(branchName: String): SelectedBranch? {
            val normalized = branchName.trim()
            return if (normalized.isValidSingleRevision()) {
                SelectedBranch(normalized, "refs/heads/$normalized")
            } else {
                null
            }
        }

        fun selectedBranch(branchRef: BranchRef): SelectedBranch? {
            val normalizedName = branchRef.name.trim()
            val normalizedRef = branchRef.refName.trim()
            return if (normalizedName.isValidSingleRevision() && normalizedRef.isValidSingleRevision()) {
                SelectedBranch(normalizedName, normalizedRef)
            } else {
                null
            }
        }

        fun selectedBranches(branchRefs: List<BranchRef>): BranchScope? {
            val branches = branchRefs
                .mapNotNull { selectedBranch(it) }
                .distinctBy { it.refName }
            return when (branches.size) {
                0 -> null
                1 -> branches.first()
                else -> SelectedBranches(branches)
            }
        }

        fun customRevisionRange(revisionRange: String): CustomRevisionRange? {
            val normalized = revisionRange.trim()
            return if (normalized.isValidSingleRevision()) {
                CustomRevisionRange(normalized)
            } else {
                null
            }
        }

        fun isValidCustomRevisionRange(revisionRange: String): Boolean {
            return revisionRange.trim().isValidSingleRevision()
        }

        private fun String.isValidSingleRevision(): Boolean {
            return isNotEmpty() &&
                this != "--" &&
                !startsWith("-") &&
                none { it.isISOControl() || it.isWhitespace() }
        }
    }
}

package com.mobilebytelabs.remoteconfig

import com.mobilebytelabs.remoteconfig.dynamic.model.UiNode
import com.mobilebytelabs.remoteconfig.dynamic.parser.UiNodeParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Forward-compatibility: a tree authored against a NEWER schema must still render on THIS build.
 *
 * The defect these lock down is specific. `parseNode` returned null for an unrecognised type and
 * `parseChildren` used `mapNotNull`, so an unknown node took its entire subtree with it — and an
 * unknown ROOT produced an empty surface. On a paywall that is a blank screen with nothing to buy,
 * reported nowhere, on a binary the user cannot update.
 */
class UiNodeDegradationTest {

    @Test
    fun unknown_node_is_substituted_not_dropped() {
        val doc = UiNodeParser.parseDocument(
            """
            {"schema_version":2,"root":{"type":"column","children":[
              {"type":"text","content":"before"},
              {"type":"holographic_carousel","whatever":123},
              {"type":"text","content":"after"}
            ]}}
            """.trimIndent(),
        )
        val column = doc.root as? UiNode.Column
        assertNotNull(column, "root failed to parse")

        // THE REGRESSION: previously 2 (the unknown vanished). Siblings must survive intact.
        assertEquals(3, column.children.size, "unknown node dropped its slot")
        val unknown = column.children[1] as? UiNode.Unknown
        assertNotNull(unknown, "middle child should be UiNode.Unknown")
        assertEquals("holographic_carousel", unknown.type)
        assertTrue(unknown.raw?.contains("whatever") == true, "raw payload retained for host handling")

        // Siblings are untouched — degradation is local to the unknown node.
        assertEquals("before", (column.children[0] as UiNode.Text).content)
        assertEquals("after", (column.children[2] as UiNode.Text).content)
    }

    @Test
    fun unknown_root_still_yields_a_tree() {
        val doc = UiNodeParser.parseDocument("""{"schema_version":9,"root":{"type":"quantum_stack"}}""")
        assertTrue(doc.root is UiNode.Unknown, "unknown root must not collapse the document to null")
        assertTrue(doc.isForwardVersion, "schema_version 9 is beyond this build")
    }

    @Test
    fun absent_schema_version_is_treated_as_v1() {
        // Trees authored before the field existed are live in production; rejecting them would
        // break exactly the consumers this library already ships to.
        val doc = UiNodeParser.parseDocument("""{"root":{"type":"text","content":"legacy"}}""")
        assertEquals(1, doc.schemaVersion)
        assertTrue(!doc.isForwardVersion)
        assertEquals("legacy", (doc.root as UiNode.Text).content)
    }

    @Test
    fun known_schema_version_is_not_forward() {
        val doc = UiNodeParser.parseDocument("""{"schema_version":2,"root":{"type":"text","content":"x"}}""")
        assertEquals(2, doc.schemaVersion)
        assertTrue(!doc.isForwardVersion)
    }
}

package com.example.newsapp.domain.util

import java.util.LinkedList
import java.util.Queue

/**
 * An optimized implementation of the Aho-Corasick automaton for multiple string matching.
 * Scans a body of text for multiple keywords simultaneously in O(C + Z) time complexity
 * where C is the length of the text and Z is the number of matches.
 */
class AhoCorasickEngine(
    dictionary: Map<String, List<String>>
) : KeywordMatcher {

    private class TrieNode {
        val children = mutableMapOf<Char, TrieNode>()
        var failureLink: TrieNode? = null
        val outputs = mutableListOf<String>() // Stores the names of the topics that match at this node
    }

    private val root = TrieNode()

    init {
        // Build the Trie
        for ((topic, keywords) in dictionary) {
            for (keyword in keywords) {
                insert(keyword.lowercase(), topic)
            }
        }
        // Wire up the failure links using BFS
        buildFailureLinks()
    }

    private fun insert(keyword: String, topic: String) {
        var current = root
        for (char in keyword) {
            current = current.children.getOrPut(char) { TrieNode() }
        }
        // At the end of the keyword, record which topic this keyword belongs to
        current.outputs.add(topic)
    }

    private fun buildFailureLinks() {
        val queue: Queue<TrieNode> = LinkedList()

        // Set failure links for depth 1 nodes to root and add to queue
        for (child in root.children.values) {
            child.failureLink = root
            queue.add(child)
        }

        while (queue.isNotEmpty()) {
            val current = queue.poll()!!

            for ((char, childNode) in current.children) {
                queue.add(childNode)

                // Traverse failure links to find the longest proper suffix
                var fallback = current.failureLink
                while (fallback != null && !fallback.children.containsKey(char)) {
                    fallback = fallback.failureLink
                }

                // If found a valid fallback, link it. Otherwise, default to root.
                childNode.failureLink = fallback?.children?.get(char) ?: root

                // Inherit outputs from the failure link (e.g. if "he" matches inside "she")
                childNode.failureLink?.outputs?.let { inheritedOutputs ->
                    childNode.outputs.addAll(inheritedOutputs)
                }
            }
        }
    }

    override fun matchFrequencies(text: String): Map<String, Int> {
        val frequencies = mutableMapOf<String, Int>()
        var current = root

        for (i in text.indices) {
            val char = text[i].lowercaseChar()

            // Follow failure links if the current character doesn't match
            while (current != root && !current.children.containsKey(char)) {
                current = current.failureLink ?: root
            }

            // Move to the child node if it exists
            current = current.children[char] ?: root

            // Tally any outputs at this state
            for (topic in current.outputs) {
                frequencies[topic] = frequencies.getOrDefault(topic, 0) + 1
            }
        }

        return frequencies
    }
}

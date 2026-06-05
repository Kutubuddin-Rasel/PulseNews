package com.example.newsapp.data.util

import org.junit.Test
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals

class TextRankSummarizerTest {

    @Test
    fun testExtractSummary() {
        val articleText = """
            On Thursday, President Donald Trump announced his administration’s latest attempt to prop up the US coal industry during an incoherent press event that randomly oscillated between energy issues and Trump’s fixation with building and renovating monuments in DC. The energy portion of the events was also frequently disconnected from reality.
            
            "Today we’re taking historic action to bring down the price of energy and the cost of living for all Americans with the power of clean, beautiful coal," said Trump, apparently unaware that coal is currently the most expensive means of generating electricity in the US.
            
            The plan, as outlined by the Department of Energy, involves using the Defense Production Act—which allows the president to direct industrial production during emergencies—to keep failing coal plants open. The administration claims this is necessary for national security, arguing that the grid needs "fuel-secure" resources like coal and nuclear power to withstand cyberattacks or extreme weather.
            
            However, grid operators and energy experts have repeatedly stated that the current mix of natural gas, renewables, and existing baseload plants is more than capable of ensuring reliability without emergency interventions. Critics argue the move is purely political, aimed at fulfilling campaign promises to coal miners despite the economic realities of the energy market.
            
            "This is a desperate attempt to save an industry that is being outcompeted by cheaper, cleaner alternatives," said a spokesperson for the Sierra Club. "Bailing out uneconomic coal plants will only raise prices for consumers and increase pollution."
        """.trimIndent()

        val title = "Trump administration invokes emergency powers to save coal plants"

        val summarizer = com.example.newsapp.data.util.nlp.TextRankSummarizer()
        val summary = summarizer.summarize(articleText, title, 3)
        
        println("Original length: ${articleText.length}")
        println("Summary length: ${summary.length}")
        println("--- SUMMARY ---")
        println(summary)
        println("---------------")
        
        assertTrue("Summary should not be empty", summary.isNotEmpty())
        assertTrue("Summary should contain bullet points", summary.contains("•"))
        assertEquals("Summary should have 3 sentences", 3, summary.split("\n").size)
    }
}

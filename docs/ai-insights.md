# Ai Insights

SprintInsightsGenerator was a first try to generate insights from sprint data. We used the same data available to a
human project-manager on the QualityBoard view.
As input, we use List<SprintStatistics> and an elaborate prompt.
We are trying to give the AI model as much data as possible to analyze the sprint situation without overloading it with
data.
This try was not successful. Partly because we used a very small model (qwen3.8-27b) not capable of this kind of
thinking. But we are also not so sure about the whole approach. What is possible with a model of this small size?
The tests we run also laked any actual meaning. How do we test such a feature? THis needs to be solve too.

A human project-manager would have to drill-down into the data in addition to the QualityBoard data:

1. check excessive unplanned overlapping sick leaves (resource drain).
2. excessive tasks where actual work exceeded the maximum estimated work (underestimation).
3. tasks that had to wait for their dependencies and therefore were delayed (domino effect).
4. tasks that where not started in-time, because their resources where busy in other projects that where delayed
   (resource drain).

We now want to try to use a little bit larger model size: qwen3.8-27b.
But we also want to step back and rethink our approach.

Can you come-up with a better approach to generate insights from sprint data using AI? How can we structure the input
data and prompts to make it more effective for the AI model to analyze and provide meaningful insights?

Are there already other open source projects that solved similar problems? If so, what can we learn from them? How can
we test the generated insights to ensure they are useful and actionable for project managers?

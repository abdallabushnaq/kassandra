# Test coverage plan

## Baseline

Measured on 2026-11-15 with JaCoCo 0.8.14 after running the GitHub Actions test
command, `mvn test -Dselenium.headless=true`, followed by
`mvn jacoco:report site -DskipTests`.

| Metric | Baseline |
|---|---:|
| Overall line coverage | 28.89% (8,508 / 29,452 lines) |
| Target | 80% line coverage |
| Additional covered lines needed at the current denominator | 15,054 |
| Tests run | 435 |
| Test result | 430 passed, 2 failed, 3 errored |

This is a provisional baseline: the run produced a JaCoCo report, but failures
should be fixed before using changes in coverage as a regression signal:

- `GanttZoomTest.rendersConfiguredZoomDetails` failed on progress-bar visibility
  at the 10.5 day-width threshold.
- `BurndownTest.burnDown` failed because the generated user's creation timestamp
  differed by six seconds.
- `GanttZoomTest.inspectsTaskPredecessors` errored on a stale Selenium element.
- `TestTest.test` errored on null user and task-start values.

Coverage in this plan means JaCoCo line coverage. The complete class-by-class
baseline below is taken from `target/site/jacoco/jacoco.csv`. Percentages are
covered executable lines divided by all executable lines. The overall rate is
the JaCoCo report-level line counter; class rows show the individual class
counters.

## Prioritized plan

1. **Stabilize the baseline tests.** Fix the failures above first, then rerun the
   same test and report commands. This ensures later coverage changes are not
   obscured by flaky or failing tests.
2. **Cover the untested Backlog UI.** Add a `BacklogViewIT` for
   `de.bushnaq.abdalla.kassandra.ui.view.Backlog` and exercise initial rendering,
   representative task/story interactions, and empty/non-empty data. It has
   0% coverage (0/889 lines). `LegacyBacklog` is also at 0% (0/939 lines);
   decide whether it remains supported before investing in a separate test.
3. **Make CI collect the intended UI integration coverage.** The current workflow
   runs `mvn test`, while `pom.xml` puts Failsafe execution behind the
   `integration-test` profile. The existing `*IT` UI tests therefore do not run
   in this coverage baseline, consistent with all 6,806 executable lines in
   `de.bushnaq.abdalla.kassandra.ui.view` currently being uncovered. Add a
   CI-safe, coverage-enabled Failsafe run for UI integration tests, explicitly
   excluding AI-service and introduction-video tests that need external
   services or interactive recording. Verify that its execution data is merged
   into the JaCoCo report before treating these tests as coverage gains.
4. **Continue through the largest uncovered UI classes.** After Backlog, add
   focused integration or component tests for `TaskGrid` (0/873 lines),
   `MainLayout` (0/473), `ActiveSprints` (0/464), and `QualityBoard` (0/444).
   Prefer behavior assertions over constructor-only coverage, and build on
   existing UI fixtures.
5. **Raise coverage in high-value dialogs and services.** Target the
   untested task/user/product dialogs and `GanttBurndownChartService`, then
   strengthen existing service and REST-controller tests around uncovered
   branches. Keep external AI, TTS, and image-generation dependencies out of
   ordinary unit tests by testing deterministic logic independently.
6. **Re-measure and reprioritize.** After each coherent batch, rerun the agreed
   CI test set and JaCoCo report; use missed executable lines and behavior risk
   to choose the next classes. Stop only when the overall line rate is at least
   80%, including all production packages in the denominator.

## Class-by-class baseline

The class names are fully qualified JaCoCo names (including `$` for nested
bytecode classes). Results are sorted by package and class name.

| Fully qualified class | Line coverage |
|---|---:|
| `de.bushnaq.abdalla.kassandra.Application` | 50.0% |
| `de.bushnaq.abdalla.kassandra.Context` | 41.7% |
| `de.bushnaq.abdalla.kassandra.KassandraParameterOptions` | 3.0% |
| `de.bushnaq.abdalla.kassandra.ParameterOptions` | 97.7% |
| `de.bushnaq.abdalla.kassandra.ai.OpenAiConfiguration` | 100.0% |
| `de.bushnaq.abdalla.kassandra.ai.filter.AiFilterGenerator` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ai.filter.AiFilterGenerator.FilterType` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ai.filter.AiFilterService` | 6.5% |
| `de.bushnaq.abdalla.kassandra.ai.filter.FilterPromptRegistry` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ai.filter.FilterPromptRegistry.PromptConfig` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ai.filter.dto.availability.AvailabilityFilterDto` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ai.filter.dto.availability.AvailabilityFilterPrompt` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ai.filter.dto.feature.FeatureFilterDto` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ai.filter.dto.feature.FeatureFilterPrompt` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ai.filter.dto.location.LocationFilterDto` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ai.filter.dto.location.LocationFilterPrompt` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ai.filter.dto.offday.OffDayFilterDto` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ai.filter.dto.offday.OffDayFilterPrompt` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ai.filter.dto.product.ProductFilterDto` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ai.filter.dto.product.ProductFilterPrompt` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ai.filter.dto.sprint.SprintFilterDto` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ai.filter.dto.sprint.SprintFilterPrompt` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ai.filter.dto.user.UserFilterDto` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ai.filter.dto.user.UserFilterPrompt` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ai.filter.dto.version.VersionFilterDto` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ai.filter.dto.version.VersionFilterPrompt` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ai.filter.java.JavaAiFilterGenerator` | 7.4% |
| `de.bushnaq.abdalla.kassandra.ai.filter.java.JavaCompilationException` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ai.filter.java.JavaFilterCompiler` | 15.8% |
| `de.bushnaq.abdalla.kassandra.ai.filter.java.JavaFilterCompiler.DynamicClassLoader` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ai.filter.java.JavaFilterCompiler.InMemoryFileManager` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ai.filter.java.JavaFilterCompiler.InMemoryJavaClassObject` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ai.filter.java.JavaFilterCompiler.InMemoryJavaFileObject` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ai.filter.js.JavaScriptAiFilterGenerator` | 7.5% |
| `de.bushnaq.abdalla.kassandra.ai.filter.js.JavaScriptExecutionValidator` | 3.2% |
| `de.bushnaq.abdalla.kassandra.ai.filter.js.JavaScriptSyntaxValidator` | 5.7% |
| `de.bushnaq.abdalla.kassandra.ai.filter.js.JavaScriptValidatorTools` | 20.0% |
| `de.bushnaq.abdalla.kassandra.ai.insights.SprintInsightsGenerator` | 8.8% |
| `de.bushnaq.abdalla.kassandra.ai.lmstudio.LmStudioCapabilities` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ai.lmstudio.LmStudioInstanceConfig` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ai.lmstudio.LmStudioLoadedInstance` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ai.lmstudio.LmStudioLoadResponse` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ai.lmstudio.LmStudioModel` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ai.lmstudio.LmStudioModelsResponse` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ai.lmstudio.LmStudioService` | 3.5% |
| `de.bushnaq.abdalla.kassandra.ai.lmstudio.LmStudioUnloadResponse` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ai.mcp.AgentThinking` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ai.mcp.AiAssistantService` | 2.2% |
| `de.bushnaq.abdalla.kassandra.ai.mcp.ApiConfiguration` | 100.0% |
| `de.bushnaq.abdalla.kassandra.ai.mcp.ContextPropagatingToolCallbackProvider` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ai.mcp.ContextPropagatingToolCallbackProvider.new ToolCallback() {...}` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ai.mcp.KassandraToolCallResultConverter` | 60.0% |
| `de.bushnaq.abdalla.kassandra.ai.mcp.KassandraToolCallResultConverter.FasterxmlColorDeserializer` | 14.3% |
| `de.bushnaq.abdalla.kassandra.ai.mcp.KassandraToolCallResultConverter.FasterxmlColorSerializer` | 20.0% |
| `de.bushnaq.abdalla.kassandra.ai.mcp.KassandraToolCallResultConverter.Initializer` | 100.0% |
| `de.bushnaq.abdalla.kassandra.ai.mcp.QueryResult` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ai.mcp.SessionToolActivityContext` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ai.mcp.ThinkingStep` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ai.mcp.ToolActivityContextHolder` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ai.mcp.ToolContextHelper` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ai.mcp.api.feature.FeatureDto` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ai.mcp.api.feature.FeatureTools` | 3.7% |
| `de.bushnaq.abdalla.kassandra.ai.mcp.api.product.ProductAclDto` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ai.mcp.api.product.ProductAclTools` | 11.8% |
| `de.bushnaq.abdalla.kassandra.ai.mcp.api.product.ProductDto` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ai.mcp.api.product.ProductTools` | 3.9% |
| `de.bushnaq.abdalla.kassandra.ai.mcp.api.sprint.SprintDto` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ai.mcp.api.sprint.SprintTools` | 5.7% |
| `de.bushnaq.abdalla.kassandra.ai.mcp.api.user.UserDto` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ai.mcp.api.user.UserTools` | 3.5% |
| `de.bushnaq.abdalla.kassandra.ai.mcp.api.usergroup.UserGroupDto` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ai.mcp.api.usergroup.UserGroupTools` | 5.7% |
| `de.bushnaq.abdalla.kassandra.ai.mcp.api.version.VersionDto` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ai.mcp.api.version.VersionTools` | 6.2% |
| `de.bushnaq.abdalla.kassandra.ai.stablediffusion.AvatarService` | 94.2% |
| `de.bushnaq.abdalla.kassandra.ai.stablediffusion.GeneratedImageResult` | 100.0% |
| `de.bushnaq.abdalla.kassandra.ai.stablediffusion.StableDiffusionConfig` | 100.0% |
| `de.bushnaq.abdalla.kassandra.ai.stablediffusion.StableDiffusionException` | 100.0% |
| `de.bushnaq.abdalla.kassandra.ai.stablediffusion.StableDiffusionOptions` | 25.0% |
| `de.bushnaq.abdalla.kassandra.ai.stablediffusion.StableDiffusionService` | 49.4% |
| `de.bushnaq.abdalla.kassandra.ai.stablediffusion.StableDiffusionService.new ParameterizedTypeReference() {...}` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ai.stablediffusion.TiledDiffusionOptions` | 71.4% |
| `de.bushnaq.abdalla.kassandra.ai.stablediffusion.sdxl.Lora` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ai.stablediffusion.sdxl.PromptGenerator` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ai.stablediffusion.sdxl.StyleProfile` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ai.stablediffusion.sdxl.StyleProfiles` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ai.stablediffusion.sdxl.WeightedRandom` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ai.stablediffusion.sdxl.WeightedRandom.WeightedOption` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ai.stablediffusion.sdxl.prompt.CameraPrompt` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ai.stablediffusion.sdxl.prompt.CozyStylePrompt` | 100.0% |
| `de.bushnaq.abdalla.kassandra.ai.stablediffusion.sdxl.prompt.CozyStylePrompt.StylePrompt` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ai.stablediffusion.sdxl.prompt.LightingPrompt` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ai.stablediffusion.sdxl.prompt.QualityPrompt` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ai.stablediffusion.sdxl.prompt.StylePrompt` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ai.tts.AbstractTtsEngine` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ai.tts.SyncResult` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ai.tts.chatterbox.ChatterboxTTS` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ai.tts.chatterbox.ChatterboxTTS.VoiceReference` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ai.tts.coqui.CoquiTTS` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ai.tts.coqui.TtsHealthInfo` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ai.tts.coqui.TtsLanguageInfo` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ai.tts.coqui.TtsModelList` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ai.tts.coqui.TtsSpeakerInfo` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ai.tts.coqui.TtsVocoderList` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ai.tts.indextts.IndexTTS` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ai.tts.indextts.IndexTTS.VoiceReference` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ai.tts.narrator.AudioMirrorRegistry` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ai.tts.narrator.AudioPlayer` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ai.tts.narrator.Narrator` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ai.tts.narrator.NarratorAttribute` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ai.tts.narrator.Playback` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ai.tts.narrator.TtsCacheManager` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ai.tts.narrator.TtsCacheManager.ChronoPlan` | 0.0% |
| `de.bushnaq.abdalla.kassandra.audit.AuditOperationContextHolder` | 100.0% |
| `de.bushnaq.abdalla.kassandra.audit.AuditRevisionEntity` | 100.0% |
| `de.bushnaq.abdalla.kassandra.audit.AuditRevisionListener` | 100.0% |
| `de.bushnaq.abdalla.kassandra.audit.PlanningOperationFilter` | 85.7% |
| `de.bushnaq.abdalla.kassandra.config.CacheConfig` | 100.0% |
| `de.bushnaq.abdalla.kassandra.config.DefaultEntitiesInitializer` | 97.5% |
| `de.bushnaq.abdalla.kassandra.config.DemoSnapshotInitializer` | 0.0% |
| `de.bushnaq.abdalla.kassandra.config.KassandraProperties` | 100.0% |
| `de.bushnaq.abdalla.kassandra.config.KassandraProperties.Gantt` | 100.0% |
| `de.bushnaq.abdalla.kassandra.config.KassandraProperties.Mode` | 100.0% |
| `de.bushnaq.abdalla.kassandra.dao.AbstractDateRangeDAO` | 100.0% |
| `de.bushnaq.abdalla.kassandra.dao.AbstractTimeAwareDAO` | 100.0% |
| `de.bushnaq.abdalla.kassandra.dao.AvailabilityDAO` | 100.0% |
| `de.bushnaq.abdalla.kassandra.dao.ErrorException` | 0.0% |
| `de.bushnaq.abdalla.kassandra.dao.FeatureDAO` | 100.0% |
| `de.bushnaq.abdalla.kassandra.dao.LocationDAO` | 100.0% |
| `de.bushnaq.abdalla.kassandra.dao.OffDayDAO` | 100.0% |
| `de.bushnaq.abdalla.kassandra.dao.OidcIdentityDAO` | 0.0% |
| `de.bushnaq.abdalla.kassandra.dao.OidcProviderDAO` | 100.0% |
| `de.bushnaq.abdalla.kassandra.dao.ProductAclEntryDAO` | 80.0% |
| `de.bushnaq.abdalla.kassandra.dao.ProductDAO` | 100.0% |
| `de.bushnaq.abdalla.kassandra.dao.RelationDAO` | 100.0% |
| `de.bushnaq.abdalla.kassandra.dao.SecurityConfigurationDAO` | 100.0% |
| `de.bushnaq.abdalla.kassandra.dao.SecurityConfigurationDAO.SetupState` | 100.0% |
| `de.bushnaq.abdalla.kassandra.dao.ServerSettingDAO` | 100.0% |
| `de.bushnaq.abdalla.kassandra.dao.SprintDAO` | 100.0% |
| `de.bushnaq.abdalla.kassandra.dao.TaskDAO` | 100.0% |
| `de.bushnaq.abdalla.kassandra.dao.UndoableOperationDAO` | 100.0% |
| `de.bushnaq.abdalla.kassandra.dao.UndoableOperationEntryDAO` | 100.0% |
| `de.bushnaq.abdalla.kassandra.dao.UserDAO` | 41.4% |
| `de.bushnaq.abdalla.kassandra.dao.UserGroupDAO` | 100.0% |
| `de.bushnaq.abdalla.kassandra.dao.VersionDAO` | 100.0% |
| `de.bushnaq.abdalla.kassandra.dao.WarnException` | 0.0% |
| `de.bushnaq.abdalla.kassandra.dao.WorkDayScheduleDAO` | 100.0% |
| `de.bushnaq.abdalla.kassandra.dao.WorklogDAO` | 100.0% |
| `de.bushnaq.abdalla.kassandra.dto.AbstractDateRange` | 100.0% |
| `de.bushnaq.abdalla.kassandra.dto.AbstractTimeAware` | 100.0% |
| `de.bushnaq.abdalla.kassandra.dto.AuditEvent` | 100.0% |
| `de.bushnaq.abdalla.kassandra.dto.AuditPage` | 100.0% |
| `de.bushnaq.abdalla.kassandra.dto.Availability` | 85.7% |
| `de.bushnaq.abdalla.kassandra.dto.Feature` | 50.0% |
| `de.bushnaq.abdalla.kassandra.dto.Location` | 87.5% |
| `de.bushnaq.abdalla.kassandra.dto.OffDay` | 63.6% |
| `de.bushnaq.abdalla.kassandra.dto.OffDayType` | 100.0% |
| `de.bushnaq.abdalla.kassandra.dto.Product` | 50.0% |
| `de.bushnaq.abdalla.kassandra.dto.ProductAclEntry` | 0.0% |
| `de.bushnaq.abdalla.kassandra.dto.ProductAclEntry.AclEntryType` | 100.0% |
| `de.bushnaq.abdalla.kassandra.dto.Relation` | 80.0% |
| `de.bushnaq.abdalla.kassandra.dto.Sprint` | 81.2% |
| `de.bushnaq.abdalla.kassandra.dto.Status` | 100.0% |
| `de.bushnaq.abdalla.kassandra.dto.Task` | 62.9% |
| `de.bushnaq.abdalla.kassandra.dto.TaskMode` | 85.7% |
| `de.bushnaq.abdalla.kassandra.dto.TaskStatus` | 100.0% |
| `de.bushnaq.abdalla.kassandra.dto.UndoRedoHistory` | 100.0% |
| `de.bushnaq.abdalla.kassandra.dto.UndoRedoHistory.EntityChange` | 100.0% |
| `de.bushnaq.abdalla.kassandra.dto.UndoRedoHistory.Operation` | 100.0% |
| `de.bushnaq.abdalla.kassandra.dto.User` | 82.1% |
| `de.bushnaq.abdalla.kassandra.dto.UserGroup` | 33.3% |
| `de.bushnaq.abdalla.kassandra.dto.UserWorkWeek` | 66.7% |
| `de.bushnaq.abdalla.kassandra.dto.Version` | 92.9% |
| `de.bushnaq.abdalla.kassandra.dto.WorkDaySchedule` | 100.0% |
| `de.bushnaq.abdalla.kassandra.dto.Worklog` | 0.0% |
| `de.bushnaq.abdalla.kassandra.dto.WorkWeek` | 90.0% |
| `de.bushnaq.abdalla.kassandra.dto.util.AvatarPrompt` | 50.0% |
| `de.bushnaq.abdalla.kassandra.dto.util.AvatarUtil` | 81.8% |
| `de.bushnaq.abdalla.kassandra.dto.util.HeaderPrompt` | 66.7% |
| `de.bushnaq.abdalla.kassandra.dto.util.IconPrompt` | 50.0% |
| `de.bushnaq.abdalla.kassandra.report.AbstractCanvas` | 66.1% |
| `de.bushnaq.abdalla.kassandra.report.LegacyAbstractChart` | 100.0% |
| `de.bushnaq.abdalla.kassandra.report.LegacyAbstractRenderer` | 93.4% |
| `de.bushnaq.abdalla.kassandra.report.burndown.DayWork` | 100.0% |
| `de.bushnaq.abdalla.kassandra.report.burndown.LegacyBurnDownChartLegacy` | 100.0% |
| `de.bushnaq.abdalla.kassandra.report.burndown.LegacyBurnDownRendererLegacy` | 63.6% |
| `de.bushnaq.abdalla.kassandra.report.burndown.RenderDao` | 100.0% |
| `de.bushnaq.abdalla.kassandra.report.calendar.CalendarUtil` | 75.0% |
| `de.bushnaq.abdalla.kassandra.report.calendar.LegacyCalendarChartLegacy` | 100.0% |
| `de.bushnaq.abdalla.kassandra.report.calendar.LegacyCalendarRendererLegacy` | 92.2% |
| `de.bushnaq.abdalla.kassandra.report.calendar.LegacyCalendarRendererLegacy.LegendItem` | 100.0% |
| `de.bushnaq.abdalla.kassandra.report.dao.AuthorContribution` | 38.1% |
| `de.bushnaq.abdalla.kassandra.report.dao.AuthorsContribution` | 50.0% |
| `de.bushnaq.abdalla.kassandra.report.dao.BurnDownGuide` | 75.8% |
| `de.bushnaq.abdalla.kassandra.report.dao.CalendarElement` | 100.0% |
| `de.bushnaq.abdalla.kassandra.report.dao.CalendarMilestoneElement` | 100.0% |
| `de.bushnaq.abdalla.kassandra.report.dao.CalendarSize` | 100.0% |
| `de.bushnaq.abdalla.kassandra.report.dao.CaptionElement` | 95.5% |
| `de.bushnaq.abdalla.kassandra.report.dao.Conflict` | 0.0% |
| `de.bushnaq.abdalla.kassandra.report.dao.ConflictingTasks` | 0.0% |
| `de.bushnaq.abdalla.kassandra.report.dao.ETheme` | 100.0% |
| `de.bushnaq.abdalla.kassandra.report.dao.FooterElement` | 90.9% |
| `de.bushnaq.abdalla.kassandra.report.dao.GraphColorUtil` | 78.7% |
| `de.bushnaq.abdalla.kassandra.report.dao.GraphSquare` | 72.7% |
| `de.bushnaq.abdalla.kassandra.report.dao.LegacyCalendarXAxes` | 90.0% |
| `de.bushnaq.abdalla.kassandra.report.dao.MapValueCopmparator` | 100.0% |
| `de.bushnaq.abdalla.kassandra.report.dao.MetaData` | 0.0% |
| `de.bushnaq.abdalla.kassandra.report.dao.Milestone` | 52.6% |
| `de.bushnaq.abdalla.kassandra.report.dao.Milestones` | 62.7% |
| `de.bushnaq.abdalla.kassandra.report.dao.Range` | 0.0% |
| `de.bushnaq.abdalla.kassandra.report.dao.ResourcesUtilization` | 50.0% |
| `de.bushnaq.abdalla.kassandra.report.dao.ResourceUtilization` | 0.0% |
| `de.bushnaq.abdalla.kassandra.report.dao.TextAlignment` | 100.0% |
| `de.bushnaq.abdalla.kassandra.report.dao.Users` | 100.0% |
| `de.bushnaq.abdalla.kassandra.report.dao.WorklogRemaining` | 100.0% |
| `de.bushnaq.abdalla.kassandra.report.dao.theme.BurndownTheme` | 87.5% |
| `de.bushnaq.abdalla.kassandra.report.dao.theme.CalendarTheme` | 100.0% |
| `de.bushnaq.abdalla.kassandra.report.dao.theme.ChartTheme` | 100.0% |
| `de.bushnaq.abdalla.kassandra.report.dao.theme.ColorConstants` | 90.0% |
| `de.bushnaq.abdalla.kassandra.report.dao.theme.DarkTheme` | 100.0% |
| `de.bushnaq.abdalla.kassandra.report.dao.theme.GanttTheme` | 100.0% |
| `de.bushnaq.abdalla.kassandra.report.dao.theme.LightTheme` | 100.0% |
| `de.bushnaq.abdalla.kassandra.report.dao.theme.SprintOverviewTheme` | 100.0% |
| `de.bushnaq.abdalla.kassandra.report.dao.theme.Theme` | 46.2% |
| `de.bushnaq.abdalla.kassandra.report.dao.theme.XAxesTheme` | 100.0% |
| `de.bushnaq.abdalla.kassandra.report.gantt.GanttContext` | 86.0% |
| `de.bushnaq.abdalla.kassandra.report.gantt.GanttUtil` | 92.5% |
| `de.bushnaq.abdalla.kassandra.report.gantt.LegacyGanttChartLegacy` | 100.0% |
| `de.bushnaq.abdalla.kassandra.report.gantt.LegacyGanttRendererLegacyLegacy` | 94.1% |
| `de.bushnaq.abdalla.kassandra.report.gantt.LegacyLegacyAbstractGanttRenderer` | 76.9% |
| `de.bushnaq.abdalla.kassandra.report.gantt.LevelingResourcesException` | 100.0% |
| `de.bushnaq.abdalla.kassandra.report.GanttBurndown.LegacyGanttBurndownChartLegacy` | 0.0% |
| `de.bushnaq.abdalla.kassandra.report.html.dao.EStatus` | 0.0% |
| `de.bushnaq.abdalla.kassandra.report.html.dao.ReportCell` | 19.0% |
| `de.bushnaq.abdalla.kassandra.report.html.dao.ReportLink` | 25.0% |
| `de.bushnaq.abdalla.kassandra.report.html.util.HtmlUtil` | 0.0% |
| `de.bushnaq.abdalla.kassandra.report.overview.LegacySprintsOverviewChartLegacy` | 0.0% |
| `de.bushnaq.abdalla.kassandra.report.overview.LegacySprintsOverviewRendererLegacy` | 0.0% |
| `de.bushnaq.abdalla.kassandra.rest.ColorDeserializer` | 54.5% |
| `de.bushnaq.abdalla.kassandra.rest.ColorSerializer` | 71.4% |
| `de.bushnaq.abdalla.kassandra.rest.ErrorResponse` | 65.1% |
| `de.bushnaq.abdalla.kassandra.rest.GlobalExceptionHandler` | 61.3% |
| `de.bushnaq.abdalla.kassandra.rest.JacksonConfig` | 100.0% |
| `de.bushnaq.abdalla.kassandra.rest.OffsetDateTimeDeserializer` | 85.7% |
| `de.bushnaq.abdalla.kassandra.rest.OffsetDateTimeSerializer` | 85.7% |
| `de.bushnaq.abdalla.kassandra.rest.RestClientConfig` | 100.0% |
| `de.bushnaq.abdalla.kassandra.rest.RestClientConfig.new DefaultResponseErrorHandler() {...}` | 33.3% |
| `de.bushnaq.abdalla.kassandra.rest.ToolsJacksonHttpMessageConverter` | 87.5% |
| `de.bushnaq.abdalla.kassandra.rest.api.AbstractApi` | 37.1% |
| `de.bushnaq.abdalla.kassandra.rest.api.AuditLogApi` | 100.0% |
| `de.bushnaq.abdalla.kassandra.rest.api.AvailabilityApi` | 86.4% |
| `de.bushnaq.abdalla.kassandra.rest.api.FeatureApi` | 50.0% |
| `de.bushnaq.abdalla.kassandra.rest.api.LocationApi` | 72.7% |
| `de.bushnaq.abdalla.kassandra.rest.api.OffDayApi` | 76.9% |
| `de.bushnaq.abdalla.kassandra.rest.api.OffDayApi.new ParameterizedTypeReference() {...}` | 100.0% |
| `de.bushnaq.abdalla.kassandra.rest.api.OidcProviderApi` | 46.2% |
| `de.bushnaq.abdalla.kassandra.rest.api.PlanningOperationContext` | 28.6% |
| `de.bushnaq.abdalla.kassandra.rest.api.ProductAclApi` | 93.3% |
| `de.bushnaq.abdalla.kassandra.rest.api.ProductApi` | 50.0% |
| `de.bushnaq.abdalla.kassandra.rest.api.ServerSettingsApi` | 88.9% |
| `de.bushnaq.abdalla.kassandra.rest.api.SprintApi` | 45.1% |
| `de.bushnaq.abdalla.kassandra.rest.api.TaskApi` | 80.9% |
| `de.bushnaq.abdalla.kassandra.rest.api.UndoRedoApi` | 85.7% |
| `de.bushnaq.abdalla.kassandra.rest.api.UserApi` | 73.4% |
| `de.bushnaq.abdalla.kassandra.rest.api.UserGroupApi` | 85.7% |
| `de.bushnaq.abdalla.kassandra.rest.api.UserWorkWeekApi` | 72.7% |
| `de.bushnaq.abdalla.kassandra.rest.api.VersionApi` | 73.7% |
| `de.bushnaq.abdalla.kassandra.rest.api.WorklogApi` | 70.6% |
| `de.bushnaq.abdalla.kassandra.rest.api.WorklogApi.new ParameterizedTypeReference() {...}` | 100.0% |
| `de.bushnaq.abdalla.kassandra.rest.api.WorkWeekApi` | 92.6% |
| `de.bushnaq.abdalla.kassandra.rest.controller.AuditLogController` | 100.0% |
| `de.bushnaq.abdalla.kassandra.rest.controller.AvailabilityController` | 97.1% |
| `de.bushnaq.abdalla.kassandra.rest.controller.AvatarProxyController` | 12.5% |
| `de.bushnaq.abdalla.kassandra.rest.controller.DarkAvatarProxyController` | 12.5% |
| `de.bushnaq.abdalla.kassandra.rest.controller.FeatureController` | 73.7% |
| `de.bushnaq.abdalla.kassandra.rest.controller.LocationController` | 91.4% |
| `de.bushnaq.abdalla.kassandra.rest.controller.OffDayController` | 92.5% |
| `de.bushnaq.abdalla.kassandra.rest.controller.OidcProviderController` | 54.5% |
| `de.bushnaq.abdalla.kassandra.rest.controller.OverviewController` | 40.0% |
| `de.bushnaq.abdalla.kassandra.rest.controller.ProductAclController` | 100.0% |
| `de.bushnaq.abdalla.kassandra.rest.controller.ProductController` | 71.5% |
| `de.bushnaq.abdalla.kassandra.rest.controller.ServerSettingsController` | 87.5% |
| `de.bushnaq.abdalla.kassandra.rest.controller.SprintController` | 71.7% |
| `de.bushnaq.abdalla.kassandra.rest.controller.TaskController` | 72.7% |
| `de.bushnaq.abdalla.kassandra.rest.controller.UndoRedoController` | 89.4% |
| `de.bushnaq.abdalla.kassandra.rest.controller.UserController` | 71.6% |
| `de.bushnaq.abdalla.kassandra.rest.controller.UserGroupController` | 88.5% |
| `de.bushnaq.abdalla.kassandra.rest.controller.UserWorkWeekController` | 92.9% |
| `de.bushnaq.abdalla.kassandra.rest.controller.VersionController` | 84.0% |
| `de.bushnaq.abdalla.kassandra.rest.controller.WorklogController` | 63.6% |
| `de.bushnaq.abdalla.kassandra.rest.controller.WorkWeekController` | 89.7% |
| `de.bushnaq.abdalla.kassandra.rest.debug.DebugUtil` | 77.8% |
| `de.bushnaq.abdalla.kassandra.rest.debug.JsonResponseLogger` | 65.4% |
| `de.bushnaq.abdalla.kassandra.rest.debug.RequestLoggingFilter` | 84.6% |
| `de.bushnaq.abdalla.kassandra.rest.debug.RequestLoggingFilter.CachedBodyHttpServletRequest` | 71.4% |
| `de.bushnaq.abdalla.kassandra.rest.debug.RequestLoggingFilter.CachedBodyHttpServletRequest.new ServletInputStream() {...}` | 40.0% |
| `de.bushnaq.abdalla.kassandra.rest.dto.gantt.AuthorSeriesDto` | 100.0% |
| `de.bushnaq.abdalla.kassandra.rest.dto.gantt.BurndownMetaDto` | 100.0% |
| `de.bushnaq.abdalla.kassandra.rest.dto.gantt.CalendarExceptionDto` | 100.0% |
| `de.bushnaq.abdalla.kassandra.rest.dto.gantt.GanttBurndownChartDto` | 100.0% |
| `de.bushnaq.abdalla.kassandra.rest.dto.gantt.GanttChartDto` | 100.0% |
| `de.bushnaq.abdalla.kassandra.rest.dto.gantt.GanttMetaDto` | 100.0% |
| `de.bushnaq.abdalla.kassandra.rest.dto.gantt.RelationDto` | 100.0% |
| `de.bushnaq.abdalla.kassandra.rest.dto.gantt.TaskDto` | 100.0% |
| `de.bushnaq.abdalla.kassandra.rest.dto.gantt.UserCalendarDto` | 100.0% |
| `de.bushnaq.abdalla.kassandra.rest.dto.overview.LaneDto` | 100.0% |
| `de.bushnaq.abdalla.kassandra.rest.dto.overview.SprintDto` | 100.0% |
| `de.bushnaq.abdalla.kassandra.rest.dto.overview.SprintOverviewDto` | 100.0% |
| `de.bushnaq.abdalla.kassandra.rest.dto.overview.SprintOverviewMetaDto` | 100.0% |
| `de.bushnaq.abdalla.kassandra.rest.dto.overview.SprintOverviewZoomSettings` | 100.0% |
| `de.bushnaq.abdalla.kassandra.rest.dto.theme.BurndownThemeDto` | 100.0% |
| `de.bushnaq.abdalla.kassandra.rest.dto.theme.CalendarThemeDto` | 100.0% |
| `de.bushnaq.abdalla.kassandra.rest.dto.theme.ChartThemeDto` | 100.0% |
| `de.bushnaq.abdalla.kassandra.rest.dto.theme.GanttThemeDto` | 100.0% |
| `de.bushnaq.abdalla.kassandra.rest.dto.theme.SprintOverviewThemeDto` | 100.0% |
| `de.bushnaq.abdalla.kassandra.rest.dto.theme.ThemeDto` | 98.1% |
| `de.bushnaq.abdalla.kassandra.rest.dto.theme.XAxesThemeDto` | 100.0% |
| `de.bushnaq.abdalla.kassandra.rest.exception.UniqueConstraintViolationException` | 54.5% |
| `de.bushnaq.abdalla.kassandra.security.CustomOidcUserService` | 20.0% |
| `de.bushnaq.abdalla.kassandra.security.DatabaseClientRegistrationRepository` | 19.0% |
| `de.bushnaq.abdalla.kassandra.security.OAuth2ClientConfig` | 100.0% |
| `de.bushnaq.abdalla.kassandra.security.OidcApiSecurityConfig` | 85.7% |
| `de.bushnaq.abdalla.kassandra.security.OidcAuthenticationManagerResolver` | 15.6% |
| `de.bushnaq.abdalla.kassandra.security.OidcClientRegistrationFactory` | 1.7% |
| `de.bushnaq.abdalla.kassandra.security.OidcClientRegistrationFactory.new ParameterizedTypeReference() {...}` | 0.0% |
| `de.bushnaq.abdalla.kassandra.security.OidcSecurityConfig` | 97.9% |
| `de.bushnaq.abdalla.kassandra.security.SecurityConfig` | 73.7% |
| `de.bushnaq.abdalla.kassandra.security.SecuritySecretService` | 71.9% |
| `de.bushnaq.abdalla.kassandra.security.SecurityUtils` | 32.4% |
| `de.bushnaq.abdalla.kassandra.security.SetupRecoveryAuthenticationProvider` | 14.3% |
| `de.bushnaq.abdalla.kassandra.service.AboutBoxService` | 79.5% |
| `de.bushnaq.abdalla.kassandra.service.AclSecurityService` | 64.9% |
| `de.bushnaq.abdalla.kassandra.service.AuditLogService` | 94.2% |
| `de.bushnaq.abdalla.kassandra.service.DatabaseDebugService` | 94.9% |
| `de.bushnaq.abdalla.kassandra.service.EnversPlanningStateService` | 100.0% |
| `de.bushnaq.abdalla.kassandra.service.EnversPlanningStateService.HistoricalState` | 100.0% |
| `de.bushnaq.abdalla.kassandra.service.GanttBurndownChartService` | 0.0% |
| `de.bushnaq.abdalla.kassandra.service.GanttBurndownExcelExportService` | 100.0% |
| `de.bushnaq.abdalla.kassandra.service.GanttChartService` | 93.2% |
| `de.bushnaq.abdalla.kassandra.service.HistoricalFieldChanges` | 76.0% |
| `de.bushnaq.abdalla.kassandra.service.OidcIdentityService` | 1.6% |
| `de.bushnaq.abdalla.kassandra.service.OidcProviderService` | 25.0% |
| `de.bushnaq.abdalla.kassandra.service.PlanningChangeService` | 88.4% |
| `de.bushnaq.abdalla.kassandra.service.PlanningChangeService.new TransactionSynchronization() {...}` | 100.0% |
| `de.bushnaq.abdalla.kassandra.service.PlanningChangeService.PendingEntry` | 100.0% |
| `de.bushnaq.abdalla.kassandra.service.ProductAclService` | 80.0% |
| `de.bushnaq.abdalla.kassandra.service.SecurityConfigurationService` | 3.1% |
| `de.bushnaq.abdalla.kassandra.service.ServerSettingsCatalogue` | 93.7% |
| `de.bushnaq.abdalla.kassandra.service.ServerSettingsCatalogue.Category` | 100.0% |
| `de.bushnaq.abdalla.kassandra.service.ServerSettingsCatalogue.Definition` | 90.0% |
| `de.bushnaq.abdalla.kassandra.service.ServerSettingsCatalogue.DefinitionBuilder` | 100.0% |
| `de.bushnaq.abdalla.kassandra.service.ServerSettingsCatalogue.Type` | 100.0% |
| `de.bushnaq.abdalla.kassandra.service.ServerSettingsService` | 93.5% |
| `de.bushnaq.abdalla.kassandra.service.SprintExportService` | 2.4% |
| `de.bushnaq.abdalla.kassandra.service.SprintsOverviewService` | 92.1% |
| `de.bushnaq.abdalla.kassandra.service.SprintsOverviewService.new SprintDAO() {...}` | 100.0% |
| `de.bushnaq.abdalla.kassandra.service.UserGroupService` | 86.0% |
| `de.bushnaq.abdalla.kassandra.service.UserRoleService` | 8.3% |
| `de.bushnaq.abdalla.kassandra.ui.HtmlColor` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ui.MainErrorHandler` | 23.1% |
| `de.bushnaq.abdalla.kassandra.ui.MainLayout` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ui.SprintStatus` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ui.component.AbstractMainGrid` | 71.7% |
| `de.bushnaq.abdalla.kassandra.ui.component.AvailabilityCalendarComponent` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ui.component.BacklogDragDropHandler` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ui.component.BacklogStoryCard` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ui.component.BacklogTaskCard` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ui.component.Breadcrumbs` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ui.component.Breadcrumbs.BreadcrumbItem` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ui.component.ChatAgentPanel` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ui.component.ChatPanelSessionState` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ui.component.ChatPanelSessionState.ChatMessageRecord` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ui.component.ChatPanelSessionState.ConversationSlot` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ui.component.CrossGridDragDropCoordinator` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ui.component.CrossGridDragDropCoordinator.CrossGridTransferEvent` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ui.component.FeatureCard` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ui.component.GlobalAiFilter` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ui.component.HelpDrawer` | 53.7% |
| `de.bushnaq.abdalla.kassandra.ui.component.HelpMarkdown` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ui.component.MergedScrumBoard` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ui.component.OffDaysCalendarComponent` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ui.component.SmartHeader` | 100.0% |
| `de.bushnaq.abdalla.kassandra.ui.component.SprintCard` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ui.component.StoryCard` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ui.component.StoryTaskCard` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ui.component.TaskCard` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ui.component.TaskClipboardHandler` | 22.0% |
| `de.bushnaq.abdalla.kassandra.ui.component.TaskClipboardHandler.new TypeReference() {...}` | 100.0% |
| `de.bushnaq.abdalla.kassandra.ui.component.TaskGrid` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ui.component.ThemeChangedEvent` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ui.component.ThemeSessionState` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ui.component.ThemeToggle` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ui.component.UndoHistoryOperationDetails` | 65.8% |
| `de.bushnaq.abdalla.kassandra.ui.component.UndoHistoryPanel` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ui.component.UserProfileChangedEvent` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ui.component.ViewToolbar` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ui.dialog.AvailabilityDialog` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ui.dialog.ConfirmDialog` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ui.dialog.DependencyDialog` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ui.dialog.FeatureDialog` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ui.dialog.ImagePromptDialog` | 44.5% |
| `de.bushnaq.abdalla.kassandra.ui.dialog.LocationDialog` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ui.dialog.OffDayDialog` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ui.dialog.ProductDialog` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ui.dialog.SprintDialog` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ui.dialog.TaskDialog` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ui.dialog.UndoHistoryConfirmationDialog` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ui.dialog.UserDialog` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ui.dialog.UserGroupDialog` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ui.dialog.UserWorkWeekDialog` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ui.dialog.VersionDialog` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ui.dialog.WorklogDialog` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ui.dialog.WorkWeekDialog` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ui.util.RenderUtil` | 31.8% |
| `de.bushnaq.abdalla.kassandra.ui.util.VaadinUtil` | 6.2% |
| `de.bushnaq.abdalla.kassandra.ui.util.VaadinUtil.DeleteValidationResult` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ui.view.AboutView` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ui.view.ActiveSprints` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ui.view.AuditLogView` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ui.view.AvailabilityListView` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ui.view.Backlog` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ui.view.FeatureListView` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ui.view.GridRowReordering` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ui.view.GroupingMode` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ui.view.InsightsView` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ui.view.Kassandra` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ui.view.LegacyBacklog` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ui.view.LegacyQualityBoard` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ui.view.LegacySprintListView` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ui.view.LocationListView` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ui.view.LoginView` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ui.view.MainView` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ui.view.OffDayListView` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ui.view.OidcProviderManagementView` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ui.view.ProductListView` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ui.view.QualityBoard` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ui.view.RecoveryView` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ui.view.ServerSettingsView` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ui.view.SetupView` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ui.view.SprintListView` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ui.view.SprintStatistics` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ui.view.UserGroupListView` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ui.view.UserListView` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ui.view.UserProfileView` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ui.view.UserWorkWeekListView` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ui.view.VersionListView` | 0.0% |
| `de.bushnaq.abdalla.kassandra.ui.view.WorkWeekListView` | 0.0% |
| `de.bushnaq.abdalla.profiler.LogLevel` | 0.0% |
| `de.bushnaq.abdalla.profiler.Profiler` | 90.2% |
| `de.bushnaq.abdalla.profiler.ProfilerResult` | 100.0% |
| `de.bushnaq.abdalla.profiler.Sample` | 85.0% |
| `de.bushnaq.abdalla.profiler.SampleSet` | 92.2% |
| `de.bushnaq.abdalla.profiler.SampleType` | 100.0% |
| `de.bushnaq.abdalla.profiler.TimeKeeping` | 0.0% |
| `de.bushnaq.abdalla.svg.util.ExtendedGraphics2D` | 28.3% |
| `de.bushnaq.abdalla.svg.util.ExtendedPolygon` | 66.7% |
| `de.bushnaq.abdalla.svg.util.ExtendedRectangle` | 45.5% |
| `de.bushnaq.abdalla.svg.util.ExtendedSvgGraphics2D` | 44.6% |
| `de.bushnaq.abdalla.svg.util.ExtendedSvgShape` | 92.3% |
| `de.bushnaq.abdalla.svg.util.RectangleWithLink` | 0.0% |
| `de.bushnaq.abdalla.svg.util.RectangleWithToolTip` | 36.4% |
| `de.bushnaq.abdalla.svg.util.SvgExtendedPolygon` | 52.9% |
| `de.bushnaq.abdalla.svg.util.SvgExtendedRectangle` | 46.9% |
| `de.bushnaq.abdalla.util.AnsiColorConstants` | 0.0% |
| `de.bushnaq.abdalla.util.ColorUtil` | 30.6% |
| `de.bushnaq.abdalla.util.Debug` | 29.0% |
| `de.bushnaq.abdalla.util.DirectoryUtil` | 0.0% |
| `de.bushnaq.abdalla.util.DurationDeserializer` | 94.7% |
| `de.bushnaq.abdalla.util.DurationSerializer` | 88.2% |
| `de.bushnaq.abdalla.util.ErrorException` | 0.0% |
| `de.bushnaq.abdalla.util.ErrorHandler` | 33.3% |
| `de.bushnaq.abdalla.util.ExcelErrorHandler` | 100.0% |
| `de.bushnaq.abdalla.util.FileUtil` | 16.5% |
| `de.bushnaq.abdalla.util.GanttErrorHandler` | 20.7% |
| `de.bushnaq.abdalla.util.MavenProperiesProvider` | 100.0% |
| `de.bushnaq.abdalla.util.MpxjUtil` | 57.1% |
| `de.bushnaq.abdalla.util.StringUtils` | 0.0% |
| `de.bushnaq.abdalla.util.TaskUtil` | 94.0% |
| `de.bushnaq.abdalla.util.Util` | 12.6% |
| `de.bushnaq.abdalla.util.date.DateTimeStruct` | 100.0% |
| `de.bushnaq.abdalla.util.date.DateUtil` | 68.0% |
| `de.bushnaq.abdalla.util.date.LocalTimeHelper` | 0.0% |
| `de.bushnaq.abdalla.util.date.ReportUtil` | 44.9% |

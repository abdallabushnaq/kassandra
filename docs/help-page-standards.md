<!--
Copyright (C) 2025-2026 Abdalla Bushnaq
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

# UI help-page design and standards

## Scope and architecture

The pilot covers the Products page (`ProductListView`) and its Create/Edit Product
dialog (`ProductDialog`). Other views and dialogs can opt in without
changing their shared base class or the main navigation.

| Surface                                                                   | Responsibility                                                                                    |
|---------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------|
| `ui\component\HelpDrawer.java`                                            | Owner-attached, non-modal Vaadin Dialog styled as a right-side drawer, with an accessible toggle. |
| `ui\component\HelpMarkdown.java`                                          | UTF-8 classpath loading, shared chapter inclusion and Vaadin's native Markdown component.         |
| `src\main\frontend\help\help-markdown.ts`                                 | Local, lazy Mermaid rendering and theme-paired screenshots.                                       |
| `src\main\frontend\help\help.css`                                         | Scoped drawer, Markdown and diagram presentation.                                                 |
| `src\main\resources\help\<route>.md`                                      | Version-controlled help content.                                                                  |
| `src\main\resources\help\product-dialog.md`                               | Create/Edit Product dialog help, separate from the list page.                                     |
| `src\main\resources\help\shared\*.md`                                     | Reusable table, filtering/sorting and Undo/Redo chapters.                                         |
| `src\main\resources\META-INF\resources\ui\images\help\light\` and `dark\` | Bundled screenshot pairs, served through the existing UI image path.                              |

No runtime connection to the wiki checkout, a CDN or a diagram service is needed.
Mermaid is pinned with Vaadin's `@NpmPackage` annotation and recorded in the npm
manifest/lockfile. Its JavaScript is dynamically imported only when a connected
help page contains a Mermaid diagram. The small help integration module and styles
are part of the normal Vaadin frontend.

The drawer overlays the right side rather than resizing the existing AI split
layout. It uses up to 72rem and fits the viewport on narrow screens. Screenshots
use the available width; prose is limited to 75 characters per line for readability.
The native non-modal Dialog overlay handles layering and keyboard/focus behavior,
including help opened from a modal product form. Opening focuses Close; Close and
Escape return focus to the invoking Help button. Navigating away closes the
view-owned drawer. Closing a product form also closes its help.

Unreadable help resources are logged and reported through a notification. Invalid
diagram content or a failed module load displays a visible error in place of the
diagram; the accompanying prose remains available.

## Adding help to a view

Create `help\<route>.md`, then attach a drawer to the view and add its toggle to the
existing smart header:

```java
HelpDrawer helpDrawer = new HelpDrawer("Products", "help/product-list.md");

addHeaderButton(helpDrawer.createToggleButton(PRODUCT_HELP_BUTTON));

add(helpDrawer);
```

Use a stable, page-specific button ID. The drawer derives its own ID from it
and uses a native dialog title. Add Javadoc to any new public constants/methods. Do not automatically
open help or persist its open state across different views.

For a dialog, attach the drawer as a child, put its toggle in the dialog header,
and close help from the owner's opened-change listener when the owner closes.
Use a dialog-specific help resource. Do not put dialog field walkthroughs in the
list page's help; explain the task there and direct users to the form's Help button.

Keep help under the view's existing authorization. Do not expose private entity
data, tokens or operational settings in static help assets. Only bundled,
reviewed help resources are accepted; this component is not an arbitrary
user-supplied Markdown viewer.

## Content template

Use short, task-oriented English with these sections when applicable:

Focus on non-obvious behavior and the user's task. Keep implementation and design
decisions in this standards document, not in user-facing help. Do not repeatedly
explain that filtering leaves data unchanged or that access limits the rows shown.
Use Kassandra's domain terminology: **product**, not project.

1. **Purpose and concepts**: what the page manages and a relationship diagram if useful.
2. **Find your way around**: annotated screenshot and short control legend.
3. **The table**: shared interaction guidance, followed by page-specific columns and row navigation.
4. **Filtering and sorting**: its own shared chapter, followed by page-specific examples.
5. **Page actions**: creating, editing and deleting, expressed as tasks and outcomes.
6. **Undo and redo**: history control, product scope and confirmation.
7. **Optional features**: availability and permission conditions.

Dialog help has its own field legend, task instructions, access defaults, optional
features, and Save/Cancel behavior. State what happens when optional fields are
left empty. For product creation, the creator is granted access automatically
and administrators have role-based access; do not describe an empty collaborator
selection as granting everybody access.

Describe controls by what users can accomplish, not merely which dialog opens:
"Edit the product name or generate a new icon/header" rather than "Opens Edit
Product." Explain deletion and its recoverability through recorded history.
Do not treat filtering, sorting or navigation as saved changes.

### Shared chapters

Include a shared chapter on its own line using a stable lowercase name:

```markdown
{{include:table}}

### Product columns and navigation

Page-specific column meanings and row navigation.

## Filtering and sorting

Filter image for this page.

{{include:filtering}}

### Product filter examples

Page-specific queries.

{{include:sorting}}

## Undo and redo

{{include:undo-redo}}
```

The renderer expands these directives from `help\shared\<name>.md` before passing
the complete document to native Markdown. Names allow only lowercase letters,
digits and hyphens. Includes are deliberately one-level: shared chapters cannot
include other files. Missing files and malformed/nested directives fail explicitly.
Do not duplicate shared chapter text across help pages or include it where the
underlying page does not support the described behavior.

The drawer supplies the page title. For ordinary help pages, start Markdown at
`##` rather than repeating a top-level title; the collapsible pilot below starts
with an introduction instead. Use ordinary headings, paragraphs, lists, tables,
images and fenced Mermaid. Avoid custom HTML and scripts.

### Collapsible chapters: Features pilot

Features opts in using `new HelpDrawer("Features", "help/feature-list.md", true)`.
The existing two-argument constructor keeps other pages and dialog help unchanged.
`HelpMarkdown` composes native Vaadin `Details` components containing native
Markdown; no custom HTML or additional dependency is needed.

Write a short, always-visible introduction before the first `##` heading.
Each `##` heading starts an initially collapsed, task-oriented chapter;
use `###` for headings inside it. Chapters can be opened independently.
**Expand all** makes the complete help readable and available to browser Find;
**Collapse all** restores the overview. Closing and reopening the same drawer
retains chapter choices; a newly created view starts collapsed.

Chapter boundaries are determined before shared includes are expanded. Included
headings remain inside their owning chapter, and headings inside backtick or tilde
code fences are not chapter boundaries. Use plain text for chapter titles.
Do not introduce nested collapsible sections or a chapter for every control.

The Features pilot groups finding/comparing work, creating/editing/deleting,
recovery, main-menu/header guidance, AI assistance and troubleshooting.
Reusable main-menu guidance lives in `help\shared\main-menu.md`; keep
page-specific creation, history and assistant context in the page's own content.
Existing screenshot pairs and theme switching still apply inside chapters.

Document observed behavior, not intended future behavior. Confirm existing
introduction-video narration against current UI code and screenshots. The pilot's
workflow reference is
`ProductsVersionsFeaturesAndSprintsIntroductionVideo09IT`: Jupiter, version
1.0.0, Config server and Minimum Viable Product. Do not copy narration verbatim;
turn it into concise task instructions.

## Referring to controls

Users do not know Java identifiers or DOM IDs. Use the exact visible label in
bold, supplemented by its icon and location where useful:

- **Create (+)** at the upper right.
- The **pencil/notepad icon** in the product row's **Actions** column.
- **Grant Access to Groups** in the Create Product dialog.

For unlabeled icons, describe both appearance and purpose. For select boxes,
give the visible field label, whether multiple values are allowed and what to
select. Do not identify a control only by color, coordinates or a screenshot
number. Explain controls that appear only with a service or permission.

Page help and filter-specific help are distinct. Page toggles use
`Help: <page>` for both the accessible name and tooltip. Existing filter help
continues to explain filter syntax.

## Screenshots

### Element-image pilot: Features

The Features help page uses independently captured controls instead of numbered
full-page images. Keep the Products pilot unchanged for comparison.
Use a control image followed by editable Markdown describing its label, location
and outcome. Images supplement the text; they must not be the only instructions.
Small controls retain their natural size rather than being enlarged to drawer width.

`GenerateScreenshotsIT.takeFeatureHelpScreenshots` captures the Features controls
in both themes without running the full wiki capture or waiting for browser closure.
It uses synthetic Jupiter / 1.0.0 / Config server data, no AI image generation.
Run it from the IDE or use the existing production frontend for a headless capture:

```bash
mvn -Pproduction test -Dtest="GenerateScreenshotsIT#takeFeatureHelpScreenshots" -Dselenium.headless=true -Dvaadin.productionMode=true
```

Generated files belong to the application, not the wiki:
`src\main\resources\META-INF\resources\ui\images\help\<theme>\FeatureListView\`.
Folder names match view class names; filenames use DOM ID values, such as
`create-feature-button.png`. Do not introduce a second set of screenshot names.
Dynamic row actions use their DOM ID prefix without the entity-specific suffix,
for example `feature-grid-edit-button-prefix-.png`. Give referenced controls a
page-specific ID when needed. Filter help uses `<filter-id>-help-button`.

Reusable capture code belongs in `SeleniumHandler`: `takeHelpElementScreenshot`,
`takeHelpRowActionScreenshot` and `takeHelpGridScreenshot`. These methods derive
the output folder and active theme automatically. Use `setHelpScreenshotTheme`
and `prepareHelpScreenshots` before capturing a page. `GenerateScreenshotsIT`
owns the page-specific element list and fixture/navigation setup only.

The capture routine explicitly lists help-referenced elements; it does not crawl
every DOM node. Buttons and selectors are captured individually; grids include
their headings and only the first populated row. Missing controls, missing rows
and invalid images fail the capture rather than producing full-page fallbacks.
Add a capture entry when documenting a new control. Regeneration overwrites the
same named assets, leaving Markdown references and other controls unaffected.
The full wiki capture also refreshes these element images when it visits Features.

```markdown
![Create feature button](images/help/light/FeatureListView/create-feature-button.png)

Use **Create (+)** at the upper right to add a feature.
```

Review and commit regenerated light/dark pairs together. Rebuild after capture
to include refreshed source images in the packaged application; an already
running server can still have the previous classpath copies. Help uses the existing
theme switching and UI image serving; no external files or new endpoint are needed.

### Full-page images

Use the wiki's `light-screenshots` and `dark-screenshots` as source material,
but copy selected images into the application. Never link to `..\kassandra.wiki`
at runtime. Check screenshots for personal/confidential data before copying.

Use matching crops and numbered callouts in both themes. Crop empty space to
keep screenshots readable in a narrow drawer. Supply a text legend with the same
numbers, descriptive alternative text and a note that example data may differ.
For dense screenshots, describe controls in prose rather than relying on tiny
labels. At desktop drawer width, callout numbers must remain readable at their
rendered size. Use detail crops if a full-page image would make them too small.
Keep referenced top-navigation controls, such as history, visible in the image.
Redact user-identifying content that is not relevant to the explanation.
Refresh both images when the referenced UI changes.

Images under `images/help/light/` or `images/help/dark/` are theme pairs. Their
theme path segment switches with the active Lumo theme. Both files must exist:

```markdown
![Create Product fields and buttons](images/help/light/product-create.png)
```

The relative URL resolves from Vaadin's `/ui/` base to `/ui/images/help/`, using
the application's existing image resource/security mapping, including deployments
using a context path. Do not introduce a separate root-level asset endpoint.
Only help image path
pairs use this convention; ordinary image links are unchanged.

## Mermaid diagrams

Vaadin 25's native Markdown renderer uses `marked` and DOMPurify; it does not
render Mermaid itself. The help component recognizes `mermaid` fenced code blocks
after native Markdown rendering and replaces them with locally generated SVG.
Ordinary code fences remain code.

Use diagrams for relationships, lifecycles or decision paths, not as substitutes
for instructions or screenshots. Prefer compact vertical flowcharts in the drawer.
Label arrows where their meaning is not obvious.

````markdown
```mermaid
flowchart TD
    accTitle: Product planning hierarchy
    accDescr: Products contain versions; versions contain features; features contain sprints.
    P["Product"] -->|contains| V["Version"]
    V -->|contains| F["Feature"]
    F -->|contains| S["Sprint"]
```

A product contains versions, each version contains features, and each feature
contains sprints.
````

Every diagram requires `accTitle`, `accDescr` and an equivalent prose explanation.
Do not encode meaning in color alone. Keep labels short and use plain text rather
than HTML labels. Check both light/dark rendering and narrow layouts.

Mermaid runs with `securityLevel: 'strict'`, `startOnLoad: false` and HTML labels
disabled. Do not add click handlers, remote resources, initialization directives
or diagram-specific security/theme overrides. No Mermaid event bindings are
installed. Rendering is scoped to the help element, serialized because Mermaid
configuration is global, and repeated from preserved source when the UI theme
changes. Observers disconnect when the help element leaves the DOM; outdated
asynchronous results are not applied.

## Review checklist

Confirm the resource loads from the packaged application; the help button, close,
Escape and focus restoration work; navigation removes the panel; both screenshot
pairs load; diagrams render in both themes; broken diagrams show an error; and
small screens remain usable. Confirm existing page actions and the AI pane still
work. Follow the repository's existing build, formatting and UI test conventions.

/*
 * Copyright (C) 2025-2026 Abdalla Bushnaq
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

type Mermaid = typeof import('mermaid')['default'];
let mermaidPromise: Promise<Mermaid> | undefined;
let renderQueue: Promise<void> = Promise.resolve();
let diagramId = 0;

function loadMermaid(): Promise<Mermaid> {
  if (!mermaidPromise) {
    mermaidPromise = import('mermaid').then((module) => module.default);
  }
  return mermaidPromise;
}

class HelpMarkdownElement extends HTMLElement {
  private readonly sources = new Map<HTMLElement, string>();
  private contentObserver?: MutationObserver;
  private themeObserver?: MutationObserver;
  private dark = false;
  private generation = 0;

  connectedCallback(): void {
    this.dark = this.isDark();
    this.contentObserver = new MutationObserver(() => this.discoverContent());
    this.contentObserver.observe(this, { childList: true, subtree: true });
    this.themeObserver = new MutationObserver(() => {
      const dark = this.isDark();
      if (dark !== this.dark) {
        this.dark = dark;
        this.updateImages();
        this.renderDiagrams();
      }
    });
    this.themeObserver.observe(document.documentElement, { attributes: true, attributeFilter: ['theme'] });
    this.themeObserver.observe(document.body, { attributes: true, attributeFilter: ['theme'] });
    this.discoverContent();
    if (this.sources.size > 0) {
      this.renderDiagrams();
    }
  }

  disconnectedCallback(): void {
    this.contentObserver?.disconnect();
    this.themeObserver?.disconnect();
    this.generation++;
  }

  private isDark(): boolean {
    return document.body.matches('[theme~="dark"]') || document.documentElement.matches('[theme~="dark"]');
  }

  private discoverContent(): void {
    let discovered = false;
    this.querySelectorAll<HTMLElement>('vaadin-markdown pre > code.language-mermaid').forEach((code) => {
      const pre = code.parentElement;
      if (pre && !this.sources.has(pre)) {
        this.sources.set(pre, code.textContent || '');
        pre.classList.add('help-diagram');
        discovered = true;
      }
    });
    this.sources.forEach((_source, element) => {
      if (!this.contains(element)) {
        this.sources.delete(element);
      }
    });
    this.updateImages();
    if (discovered) {
      this.renderDiagrams();
    }
  }

  private updateImages(): void {
    this.querySelectorAll<HTMLImageElement>('vaadin-markdown img').forEach((image) => {
      const source = image.getAttribute('src');
      if (source && /(^|\/)images\/help\/(light|dark)\//.test(source)) {
        const themed = source.replace(/\/(light|dark)\//, this.dark ? '/dark/' : '/light/');
        if (themed !== source) {
          image.setAttribute('src', themed);
        }
      }
    });
  }

  private renderDiagrams(): void {
    const generation = ++this.generation;
    const dark = this.dark;
    // Mermaid configuration is global: serialize initialization and rendering across drawers.
    renderQueue = renderQueue.then(async () => {
      for (const [element, source] of this.sources) {
        if (!this.isConnected || generation !== this.generation) {
          return;
        }
        let id: string | undefined;
        try {
          const mermaid = await loadMermaid();
          if (!this.isConnected || generation !== this.generation) {
            return;
          }
          mermaid.initialize({
            startOnLoad: false,
            securityLevel: 'strict',
            theme: dark ? 'dark' : 'default',
            suppressErrorRendering: true,
            flowchart: { htmlLabels: false },
          });
          id = `help-mermaid-${++diagramId}`;
          const { svg } = await mermaid.render(id, source);
          if (!this.isConnected || generation !== this.generation) {
            return;
          }
          element.innerHTML = svg;
          element.classList.remove('help-diagram-error');
          element.removeAttribute('role');
        } catch (error) {
          console.error('Unable to render help diagram', error);
          if (id) {
            document.getElementById(`d${id}`)?.remove();
          }
          if (this.isConnected && generation === this.generation) {
            element.textContent = 'This diagram could not be displayed. Please use the explanation below.';
            element.classList.add('help-diagram-error');
            element.setAttribute('role', 'alert');
          }
        }
      }
    });
  }
}

customElements.define('kassandra-help-markdown', HelpMarkdownElement);

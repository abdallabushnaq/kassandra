// sprints-overview/context-menu.ts
// Singleton context menu for right-clicking sprint rectangles.
//
// Copyright (C) 2025-2026 Abdalla Bushnaq – Apache License 2.0

let contextMenuSingleton: HTMLDivElement | null = null;

function getOrCreateContextMenu(): HTMLDivElement {
    if (contextMenuSingleton) return contextMenuSingleton;
    const menu = document.createElement('div');
    menu.style.cssText = [
        'position:fixed', 'z-index:99999',
        'background:var(--lumo-base-color,#fff)',
        'color:var(--lumo-body-text-color,#333)',
        'border:1px solid var(--lumo-contrast-20pct,#ccc)',
        'border-radius:var(--lumo-border-radius-m,4px)',
        'box-shadow:0 4px 16px rgba(0,0,0,.18)',
        'padding:4px 0', 'min-width:160px', 'display:none',
    ].join(';');
    document.body.appendChild(menu);
    contextMenuSingleton = menu;
    document.addEventListener('click', (e) => {
        if (contextMenuSingleton && !contextMenuSingleton.contains(e.target as Node)) {
            contextMenuSingleton.style.display = 'none';
        }
    });
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape' && contextMenuSingleton) contextMenuSingleton.style.display = 'none';
    });
    return menu;
}

export function hideContextMenu(): void {
    if (contextMenuSingleton) contextMenuSingleton.style.display = 'none';
}

export interface SprintMenuItem {
    id:     number | string;
    name?:  string;
    key?:   string;
    status?: string;
}

export function showContextMenuForSprint(clientX: number, clientY: number, sprint: SprintMenuItem): void {
    const menu = getOrCreateContextMenu();
    menu.innerHTML = '';

    if (sprint.name) {
        const header = document.createElement('div');
        header.style.cssText = 'padding:4px 14px;font-weight:bold;border-bottom:1px solid var(--lumo-contrast-20pct,#ccc);margin-bottom:4px';
        header.textContent = sprint.name;
        menu.appendChild(header);
    }

    const items = [
        { label: 'Backlog',        href: `/ui/backlog?sprint=${sprint.id}` },
        { label: 'Active Sprint',  href: `/ui/active-sprint?sprint=${sprint.id}` },
        { label: 'Quality Board',  href: `/ui/quality-board?sprint=${sprint.id}` },
    ];
    for (const item of items) {
        const row = document.createElement('a');
        row.href        = item.href;
        row.textContent = item.label;
        row.style.cssText = 'display:block;padding:6px 14px;text-decoration:none;color:inherit;cursor:pointer';
        row.addEventListener('mouseenter', () => { row.style.background = 'var(--lumo-primary-color-10pct,#e8f0fe)'; });
        row.addEventListener('mouseleave', () => { row.style.background = ''; });
        row.addEventListener('click', (e) => { e.stopPropagation(); hideContextMenu(); });
        menu.appendChild(row);
    }

    menu.style.display = 'block';
    const menuW   = menu.offsetWidth  || 160;
    const menuH   = menu.offsetHeight || 120;
    const leftPos = clientX + menuW + 2 > window.innerWidth  ? clientX - menuW : clientX + 2;
    const topPos  = clientY + menuH + 2 > window.innerHeight ? clientY - menuH : clientY + 2;
    menu.style.left = Math.max(0, leftPos) + 'px';
    menu.style.top  = Math.max(0, topPos)  + 'px';
}


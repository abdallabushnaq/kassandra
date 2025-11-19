import { css, registerStyles } from '@vaadin/vaadin-themable-mixin';

registerStyles('vaadin-grid', css`
  /* Jira-style backlog grid styles for vaadin-grid */

    vaadin-grid {
        background: transparent;
        border: 0;
    }

    [part~="row"] {
        padding-inline: 1px;
        background: transparent;
    }

    [part~="body-row"] {
        padding-block: 1px;
        filter: drop-shadow(0px 2px 2px hsla(0deg, 0%, 0%, 0.2));
    }

    [part~="body-cell"] {
        background: #ffffff !important;
        border-block: 1px solid var(--lumo-contrast-30pct);
    }

    [part~="body-cell"][part~="first-column-cell"] {
        border-start-start-radius: 4px;
        border-end-start-radius: 4px;
        border-inline-start: 1px solid var(--lumo-contrast-30pct);
    }

    [part~="body-cell"][part~="last-column-cell"] {
        border-start-end-radius: 4px;
        border-end-end-radius: 4px;
        border-inline-end: 1px solid var(--lumo-contrast-30pct);
    }

    [part~="selected-row"] [part~="cell"] {
        background: var(--lumo-primary-color-50pct) !important;
    }

/*    [part~="selected-row"]:hover [part~="cell"] {
        background: var(--_lumo-grid-selected-row-color);
    }*/

/*    [part~="selected-row"] [part~="cell"] {
        background: #e3f2fd !important;
    }

    [part~="selected-row"]:hover [part~="cell"] {
        background: #bbdefb !important;
    }*/

`);


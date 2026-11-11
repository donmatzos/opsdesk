export interface NavItem {
  label: string;
  route: string;
  icon: string;
}

export interface NavGroup {
  label: string;
  items: NavItem[];
}

/** Single source for the sidebar; routes must exist in app.routes.ts. */
export const NAV_GROUPS: NavGroup[] = [
  {
    label: 'Dashboard',
    items: [
      { label: 'Overview', route: '/overview', icon: 'layout-dashboard' },
      { label: 'Services', route: '/services', icon: 'server' },
      { label: 'Incidents', route: '/incidents', icon: 'triangle-alert' },
      { label: 'Ask the runbooks', route: '/assist', icon: 'message-square' },
    ],
  },
  {
    label: 'Project guide',
    items: [
      { label: 'Introduction', route: '/guide', icon: 'book-open' },
      { label: 'Architecture', route: '/guide/architecture', icon: 'layers' },
      { label: 'What this shows', route: '/guide/skills', icon: 'list-checks' },
      { label: 'Design and accessibility', route: '/guide/design', icon: 'eye' },
      { label: 'Glossary', route: '/guide/glossary', icon: 'list' },
    ],
  },
];

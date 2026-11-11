// Static demo build (GitHub Pages): replaces environment.ts via angular.json fileReplacements ("demo" configuration).
// demo: true registers the in-browser API mock, so no backend is needed.
export const environment = {
  demo: true,
  homeRoute: 'guide',
  repoUrl: 'https://github.com/donmatzos/opsdesk',
  repoRef: 'main',
};

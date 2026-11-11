import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-not-found-page',
  imports: [RouterLink],
  template: `
    <h1>Page not found</h1>
    <p>This page does not exist. <a routerLink="/">Go to the start page</a>.</p>
  `,
})
export class NotFoundPage {}

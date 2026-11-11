import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

/** Loads a Markdown file of docs/guide, served as an asset below guide/ (relative, so <base href> applies). */
@Injectable({ providedIn: 'root' })
export class GuideContent {
  private readonly http = inject(HttpClient);

  load(file: string): Observable<string> {
    return this.http.get(`guide/${file}`, { responseType: 'text' });
  }
}

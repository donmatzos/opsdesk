import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { FocusOnNavigate } from './focus-on-navigate';

@Component({ template: `` })
class Dummy {}

describe('FocusOnNavigate', () => {
  it('focuses main on the second navigation, not on the first', async () => {
    TestBed.configureTestingModule({
      providers: [provideRouter([{ path: 'a', component: Dummy }, { path: 'b', component: Dummy }])],
    });
    const main = document.createElement('main');
    main.id = 'main';
    main.tabIndex = -1;
    document.body.appendChild(main);
    try {
      TestBed.inject(FocusOnNavigate);
      const router = TestBed.inject(Router);
      await router.navigateByUrl('/a');
      expect(document.activeElement).not.toBe(main);
      await router.navigateByUrl('/b');
      expect(document.activeElement).toBe(main);
    } finally {
      main.remove();
    }
  });
});

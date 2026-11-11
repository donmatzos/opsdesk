import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Title } from '@angular/platform-browser';
import { Router, TitleStrategy, provideRouter } from '@angular/router';
import { OpsdeskTitleStrategy } from './title-strategy';

@Component({ template: `` })
class Dummy {}

describe('OpsdeskTitleStrategy', () => {
  it('sets "<route title> · opsdesk"', async () => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: 'incidents', component: Dummy, title: 'Incidents' }, { path: 'plain', component: Dummy }]),
        { provide: TitleStrategy, useExisting: OpsdeskTitleStrategy },
      ],
    });
    const router = TestBed.inject(Router);
    await router.navigateByUrl('/incidents');
    expect(TestBed.inject(Title).getTitle()).toBe('Incidents · opsdesk');
    await router.navigateByUrl('/plain');
    expect(TestBed.inject(Title).getTitle()).toBe('opsdesk');
  });
});

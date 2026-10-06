import { AbstractControl, ValidationErrors } from '@angular/forms';

/**
 * Like Validators.required, but also rejects whitespace-only input
 * (mirrors @NotBlank in the backend, which would otherwise answer with HTTP 400).
 */
export function notBlank(control: AbstractControl): ValidationErrors | null {
  const value = control.value;
  return typeof value === 'string' && value.trim().length === 0 ? { required: true } : null;
}

import type { AppPath } from '../constants/paths';

export function navigateWithTransition(path: AppPath, navigate: (path: AppPath) => void) {
  navigate(path);
}

export const stripVietnameseMarks = (value: string) =>
  value
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/đ/g, 'd')
    .replace(/Đ/g, 'D');

export const normalizeSearchText = (value?: string | null) =>
  stripVietnameseMarks(value ?? '').trim().toLowerCase();

export const searchMatches = (text: string, keyword: string) => {
  const trimmedKeyword = keyword.trim().toLowerCase();
  if (!trimmedKeyword) {
    return true;
  }
  return normalizeSearchText(text).includes(normalizeSearchText(trimmedKeyword)) || text.toLowerCase().includes(trimmedKeyword);
};

export const antSelectFilterOption = (input: string, option?: { label?: unknown; value?: unknown }) =>
  searchMatches(String(option?.label ?? option?.value ?? ''), input);

export type WorkloadRule = {
  code: string;
  name: string;
  coefficientTheoryFormula?: string | null;
  coefficientPracticeFormula?: string | null;
  formula: string;
  note: string;
  editable: boolean;
};

export type SystemSetting = {
  key: string;
  name: string;
  value: string;
  description: string;
};

export const WORKLOAD_RULES_STORAGE_KEY = 'klgd_workload_rules';
export const SYSTEM_SETTINGS_STORAGE_KEY = 'klgd_system_settings';
export const FORMULA_VARIABLES = {
  standardHours: 'GC',
  students: 'SV',
  credits: 'TC',
  commonK: 'K',
  theoryK: 'K_lt',
  practiceK: 'K_th',
  groupStudents: 'SV_nhom',
  weeks: 'T',
  days: 'N',
  advisorShare: 'HSHD'
} as const;
export const FORMULA_VARIABLE_NOTE = 'Ký hiệu: GC = giờ chuẩn; SV = số sinh viên; TC = tín chỉ; K = hệ số chung; K_lt = hệ số lý thuyết; K_th = hệ số thực hành/thí nghiệm; SV_nhom = số SV trong nhóm thí nghiệm; T = số tuần; N = số ngày; HSHD = hệ số hướng dẫn.';

export const DEFAULT_WORKLOAD_RULES: WorkloadRule[] = [
  {
    code: 'LY_THUYET_BAI_TAP',
    name: 'Lý thuyết, bài tập',
    formula: 'K = min(max(1.0 + (SV - 40) × 0.01, 0.9), 1.5); GC = TC × 15 × K',
    note: 'Áp dụng cho lớp lý thuyết hoặc bài tập thông thường.',
    editable: true
  },
  {
    code: 'THI_NGHIEM',
    name: 'Thí nghiệm, thực hành',
    formula: 'K = 0.6 + (0.6 + (SV - 25 - 25) × 0.015); GC = TC × 15 × K',
    note: 'Áp dụng cho học phần thí nghiệm, thực hành hoặc bài tập có quy đổi.',
    editable: true
  },
  {
    code: 'DO_AN',
    name: 'Đồ án',
    formula: 'K = min(max(1.1 + (SV - 40) × 0.01, 1.0), 1.5); GC = TC × 15 × K',
    note: 'Áp dụng cho đồ án môn học, đồ án chuyên ngành.',
    editable: true
  },
  {
    code: 'THUC_TAP_TOT_NGHIEP',
    name: 'Thực tập nghề nghiệp/tốt nghiệp',
    formula: 'K = 0.5; GC = K × SV × TC',
    note: 'Số tuần lấy bằng số tín chỉ của học phần thực tập.',
    editable: true
  },
  {
    code: 'THUC_TAP_NGANH',
    name: 'Thực tập ngành',
    formula: 'K = 0.5; GC = K × SV × TC',
    note: 'Số tuần lấy bằng số tín chỉ của học phần thực tập.',
    editable: true
  },
  {
    code: 'HPTN',
    name: 'Học phần tốt nghiệp',
    formula: 'KT/CN: GC = SV × 14 × HSHD; KT-XH-NNA: GC = SV × 14 × 0.8 × HSHD',
    note: 'KT/CN = kỹ thuật/công nghệ; KT-XH-NNA = kinh tế, xã hội, ngôn ngữ Anh. Một SV có 2 GV hướng dẫn thì HSHD của mỗi GV là 0.5.',
    editable: true
  },
  {
    code: 'THUC_TAP_TRAC_DIA',
    name: 'Thực tập trắc địa',
    formula: 'Nếu SV < 20 thì K = 2.0, ngược lại K = 2.5 + (SV - 25) × 0.05; GC = K × N',
    note: 'N lấy từ cấu hình số ngày thực tập trắc địa.',
    editable: true
  },
  {
    code: 'THUC_TAP_TRUC_TIEP',
    name: 'Thực tập trực tiếp',
    formula: 'Nếu SV < 30 thì K = 2.0, ngược lại K = 2.5 + (SV - 35) × 0.05; GC = K × N',
    note: 'N lấy từ cấu hình số ngày thực tập mặc định.',
    editable: true
  },
  {
    code: 'NGOAI_NGU',
    name: 'Ngoại ngữ',
    formula: 'K = min(max(1.0 + (SV - 40) × 0.01, 0.9), 1.5); GC = TC × 15 × K',
    note: 'Áp dụng theo tên học phần hoặc bộ môn ngoại ngữ.',
    editable: true
  },
  {
    code: 'GDTC',
    name: 'Giáo dục thể chất',
    coefficientTheoryFormula: 'K_lt = min(max(1.0 + (SV - 50) × 0.01, 0.9), 1.2)',
    coefficientPracticeFormula: 'K_th = min(max(0.7 + (SV - 50) × 0.01, 0.6), 0.9)',
    formula: 'GC = 20 × K_lt + 10 × K_th',
    note: 'Áp dụng cho bộ môn Giáo dục thể chất thuộc khối khoa học cơ bản.',
    editable: true
  },
  {
    code: 'VAT_LIEU_XAY_DUNG',
    name: 'Vật liệu xây dựng',
    coefficientTheoryFormula: 'K_lt = 1.0 + (SV - 40) × 0.01',
    coefficientPracticeFormula: 'K_th = max(0.6 + (SV - 25) × 0.015, 0.5)',
    formula: 'GC = 42 × K_lt + 9 × K_th',
    note: 'Theo đề cương VLXD: 42 tiết lý thuyết và 9 tiết thí nghiệm; lớp thí nghiệm tách theo nhóm 25 SV.',
    editable: true
  },
  {
    code: 'CO_HOC_DAT',
    name: 'Cơ học đất',
    coefficientTheoryFormula: 'K_lt = 1.0 + (SV - 40) × 0.01',
    coefficientPracticeFormula: 'K_th = max(0.6 + (SV - 25) × 0.015, 0.5)',
    formula: 'GC = 39 × K_lt + 6 × K_th',
    note: 'Theo đề cương: LT 30, BT 9, TN 6; phần TN tính riêng giống VLXD.',
    editable: true
  },
  {
    code: 'DIA_KY_THUAT',
    name: 'Địa kỹ thuật',
    coefficientTheoryFormula: 'K_lt = 1.0 + (SV - 40) × 0.01',
    coefficientPracticeFormula: 'K_th = max(0.6 + (SV - 25) × 0.015, 0.5)',
    formula: 'GC = 54 × K_lt + 6 × K_th',
    note: 'Theo đề cương: LT 54, TN 6; phần TN tính riêng giống VLXD.',
    editable: true
  },
  {
    code: 'THINH_GIANG',
    name: 'Thỉnh giảng',
    formula: 'GC = TC × 15',
    note: 'Áp dụng khi dữ liệu xác định giảng viên hoặc lớp thỉnh giảng.',
    editable: true
  }
];

export const DEFAULT_SYSTEM_SETTINGS: SystemSetting[] = [
  {
    key: 'academicYear',
    name: 'Năm học mặc định',
    value: '2025-2026',
    description: 'Dùng khi tạo lần nhập dữ liệu mới.'
  },
  {
    key: 'semester',
    name: 'Học kỳ mặc định',
    value: 'Kỳ 1',
    description: 'Dùng khi tạo lần nhập dữ liệu mới.'
  },
  {
    key: 'detectGuestByPosition',
    name: 'Nhận diện thỉnh giảng theo chức vụ',
    value: 'false',
    description: 'Bật nếu muốn hệ thống nhận diện thỉnh giảng theo cột chức vụ.'
  },
  {
    key: 'graduationInternshipWeeks',
    name: 'Số tuần thực tập tốt nghiệp',
    value: '28',
    description: 'Tham số dùng trong công thức tính thực tập tốt nghiệp.'
  },
  {
    key: 'defaultInternshipDays',
    name: 'Số ngày thực tập mặc định',
    value: '2',
    description: 'Tham số dùng trong công thức tính thực tập trực tiếp.'
  },
  {
    key: 'surveyingInternshipDays',
    name: 'Số ngày thực tập trắc địa',
    value: '7',
    description: 'Tham số dùng trong công thức tính thực tập trắc địa.'
  },
  {
    key: 'materialTheoryHours',
    name: 'VLXD: tiết lý thuyết/bài tập',
    value: '42',
    description: 'Dùng trong công thức VLXD.'
  },
  {
    key: 'materialLabHours',
    name: 'VLXD: tiết thí nghiệm mỗi nhóm',
    value: '9',
    description: 'Dùng trong công thức VLXD.'
  },
  {
    key: 'soilTheoryHours',
    name: 'Cơ học đất: tiết lý thuyết/bài tập',
    value: '39',
    description: 'LT 30 + BT 9 theo đề cương.'
  },
  {
    key: 'soilLabHours',
    name: 'Cơ học đất: tiết thí nghiệm mỗi nhóm',
    value: '6',
    description: 'Dùng trong công thức Cơ học đất.'
  },
  {
    key: 'geotechnicalTheoryHours',
    name: 'Địa kỹ thuật: tiết lý thuyết/bài tập',
    value: '54',
    description: 'LT 54 theo đề cương.'
  },
  {
    key: 'geotechnicalLabHours',
    name: 'Địa kỹ thuật: tiết thí nghiệm mỗi nhóm',
    value: '6',
    description: 'Dùng trong công thức Địa kỹ thuật.'
  },
  {
    key: 'labGroupSize',
    name: 'Số SV tối đa mỗi nhóm thí nghiệm',
    value: '25',
    description: 'Dùng để tách nhóm thí nghiệm khi tính Ktn.'
  },
  {
    key: 'labBaseK',
    name: 'Ktn gốc',
    value: '0.6',
    description: 'Ktn = K gốc + (SV nhóm - SV gốc) × bước tăng.'
  },
  {
    key: 'labBaseStudents',
    name: 'SV gốc của Ktn',
    value: '25',
    description: 'Mốc sinh viên của công thức K thí nghiệm.'
  },
  {
    key: 'labIncrementPerStudent',
    name: 'Bước tăng Ktn mỗi SV',
    value: '0.015',
    description: 'Bước tăng hệ số K thí nghiệm.'
  },
  {
    key: 'labMinK',
    name: 'Ktn tối thiểu',
    value: '0.5',
    description: 'Ngưỡng dưới hệ số K thí nghiệm.'
  },
  {
    key: 'labMaxK',
    name: 'Ktn tối đa',
    value: '1.2',
    description: 'Ngưỡng trên hệ số K thí nghiệm.'
  },
  {
    key: 'theoryBaseK',
    name: 'Klt gốc',
    value: '1',
    description: 'Klt = K gốc + (SV - SV gốc) × bước tăng.'
  },
  {
    key: 'theoryBaseStudents',
    name: 'SV gốc của Klt',
    value: '40',
    description: 'Mốc sinh viên của công thức K lý thuyết.'
  },
  {
    key: 'theoryIncrementPerStudent',
    name: 'Bước tăng Klt mỗi SV',
    value: '0.01',
    description: 'Bước tăng hệ số K lý thuyết.'
  },
  {
    key: 'theoryMinK',
    name: 'Klt tối thiểu',
    value: '0.9',
    description: 'Ngưỡng dưới hệ số K lý thuyết.'
  },
  {
    key: 'theoryMaxK',
    name: 'Klt tối đa',
    value: '1.5',
    description: 'Ngưỡng trên hệ số K lý thuyết.'
  }
];

const readStorage = <T,>(key: string, fallback: T): T => {
  try {
    const raw = window.localStorage.getItem(key);
    if (raw && /\u00c3|\u00c4|\u00c6|\u00e1\u00ba|\u00e1\u00bb/.test(raw)) {
      window.localStorage.removeItem(key);
      return fallback;
    }
    return raw ? (JSON.parse(raw) as T) : fallback;
  } catch {
    return fallback;
  }
};

const abbreviateFormulaText = (value?: string | null) => (value ?? '')
  .replace(/giờ chuẩn/gi, 'GC')
  .replace(/gio chuan/gi, 'GC')
  .replace(/tín chỉ/gi, 'TC')
  .replace(/tin chi/gi, 'TC')
  .replace(/hệ số hướng dẫn/gi, 'HSHD')
  .replace(/he so huong dan/gi, 'HSHD')
  .replace(/số tuần thực tập tốt nghiệp/gi, 'T')
  .replace(/so tuan thuc tap tot nghiep/gi, 'T')
  .replace(/số ngày thực tập trắc địa/gi, 'N')
  .replace(/so ngay thuc tap trac dia/gi, 'N')
  .replace(/số ngày thực tập mặc định/gi, 'N')
  .replace(/so ngay thuc tap mac dinh/gi, 'N')
  .trim();

const abbreviateRule = (rule: WorkloadRule): WorkloadRule => ({
  ...rule,
  coefficientTheoryFormula: abbreviateFormulaText(rule.coefficientTheoryFormula),
  coefficientPracticeFormula: abbreviateFormulaText(rule.coefficientPracticeFormula),
  formula: abbreviateFormulaText(rule.formula)
});

export const readWorkloadRules = () => {
  const storedRules = readStorage(WORKLOAD_RULES_STORAGE_KEY, DEFAULT_WORKLOAD_RULES);
  const refreshedCodes = new Set(DEFAULT_WORKLOAD_RULES.map((rule) => rule.code));
  const byCode = new Map(storedRules.map((rule) => [rule.code, rule]));
  const migratedRules = [
    ...DEFAULT_WORKLOAD_RULES.map((defaultRule) =>
      refreshedCodes.has(defaultRule.code) ? defaultRule : (byCode.get(defaultRule.code) ?? defaultRule)
    ),
    ...storedRules.filter((rule) => !DEFAULT_WORKLOAD_RULES.some((defaultRule) => defaultRule.code === rule.code))
  ].map(abbreviateRule);
  if (JSON.stringify(migratedRules) !== JSON.stringify(storedRules)) {
    saveWorkloadRules(migratedRules);
  }
  return migratedRules;
};

export const saveWorkloadRules = (rules: WorkloadRule[]) => {
  window.localStorage.setItem(WORKLOAD_RULES_STORAGE_KEY, JSON.stringify(rules.map(abbreviateRule)));
};

export const readSystemSettings = () => {
  const storedSettings = readStorage(SYSTEM_SETTINGS_STORAGE_KEY, DEFAULT_SYSTEM_SETTINGS);
  const migratedSettings = storedSettings.map((setting) => {
    if (setting.key === 'geotechnicalTheoryHours' && setting.value === '57') {
      return { ...setting, value: '54', description: 'LT 54 theo đề cương.' };
    }
    if (setting.key === 'labMaxK' && setting.value === '999') {
      return { ...setting, value: '1.2', description: 'Ngưỡng trên hệ số K thí nghiệm.' };
    }
    return setting;
  });
  const existingKeys = new Set(migratedSettings.map((setting) => setting.key));
  for (const defaultSetting of DEFAULT_SYSTEM_SETTINGS) {
    if (!existingKeys.has(defaultSetting.key)) {
      migratedSettings.push(defaultSetting);
    }
  }
  if (JSON.stringify(migratedSettings) !== JSON.stringify(storedSettings)) {
    saveSystemSettings(migratedSettings);
  }
  return migratedSettings;
};

export const saveSystemSettings = (settings: SystemSetting[]) => {
  window.localStorage.setItem(SYSTEM_SETTINGS_STORAGE_KEY, JSON.stringify(settings));
};

const toNumber = (value: string) => Number(value.replace(',', '.'));
const extractNumbers = (formula?: string) => (formula?.match(/-?\d+(?:[.,]\d+)?/g) ?? []).map(toNumber);
const ruleByCode = (rules: WorkloadRule[], code: string) => rules.find((rule) => rule.code === code);
const formulaLine = (rule: WorkloadRule | undefined, matcher: RegExp) =>
  [rule?.coefficientTheoryFormula, rule?.coefficientPracticeFormula, rule?.formula]
    .filter(Boolean)
    .join('; ')
    .split(';')
    .map((line) => line.trim())
    .find((line) => matcher.test(line));

const parseKFormula = (line?: string) => {
  const numbers = extractNumbers(line);
  const normalizedLine = line?.replace(/\u00d7/g, '*') ?? '';
  const linearMatch = normalizedLine.match(/([0-9]+(?:[.,][0-9]+)?)\s*\+\s*\(\s*SV(?:_nhom)?\s*-\s*([0-9]+(?:[.,][0-9]+)?)\s*\)\s*\*\s*([0-9]+(?:[.,][0-9]+)?)/i);
  if (!linearMatch || numbers.length < 3) {
    return undefined;
  }
  const hasMinMaxClamp = /min\s*\(/i.test(normalizedLine) && /max\s*\(/i.test(normalizedLine);
  const hasMinClampOnly = /max\s*\(/i.test(normalizedLine);
  return {
    baseK: toNumber(linearMatch[1]),
    baseStudents: toNumber(linearMatch[2]),
    incrementPerStudent: toNumber(linearMatch[3]),
    minK: hasMinMaxClamp ? (numbers[numbers.length - 2] ?? 0) : hasMinClampOnly ? (numbers[numbers.length - 1] ?? 0) : 0,
    maxK: hasMinMaxClamp ? (numbers[numbers.length - 1] ?? 999) : 999
  };
};

const parseSpecialHours = (rule: WorkloadRule | undefined) => {
  const totalLine = formulaLine(rule, /giờ chuẩn|gio chuan|\bGC\b/i);
  const numbers = extractNumbers(totalLine);
  if (numbers.length < 2) {
    return undefined;
  }
  return {
    theoryHours: numbers[0],
    labHours: numbers[1],
    groupSize: numbers[2]
  };
};

export const buildCalculationSettings = (settings: SystemSetting[]) => {
  const valueOf = (key: string, fallback: string) => settings.find((item) => item.key === key)?.value ?? fallback;
  const rules = readWorkloadRules();
  const theoryK = parseKFormula(formulaLine(ruleByCode(rules, 'LY_THUYET_BAI_TAP'), /K\s*=/i) ?? formulaLine(ruleByCode(rules, 'VAT_LIEU_XAY_DUNG'), /K_lt|Klt/i));
  const labK = parseKFormula(formulaLine(ruleByCode(rules, 'THI_NGHIEM'), /K\s*=/i) ?? formulaLine(ruleByCode(rules, 'VAT_LIEU_XAY_DUNG'), /K_th|Ktn/i));
  const materialHours = parseSpecialHours(ruleByCode(rules, 'VAT_LIEU_XAY_DUNG'));
  const soilHours = parseSpecialHours(ruleByCode(rules, 'CO_HOC_DAT'));
  const geotechnicalHours = parseSpecialHours(ruleByCode(rules, 'DIA_KY_THUAT'));

  return {
    detectGuestByPosition: valueOf('detectGuestByPosition', 'false') === 'true',
    graduationInternshipWeeks: Number(valueOf('graduationInternshipWeeks', '28')),
    defaultInternshipDays: Number(valueOf('defaultInternshipDays', '2')),
    surveyingInternshipDays: Number(valueOf('surveyingInternshipDays', '7')),
    materialTheoryHours: materialHours?.theoryHours ?? Number(valueOf('materialTheoryHours', '42')),
    materialLabHours: materialHours?.labHours ?? Number(valueOf('materialLabHours', '9')),
    soilTheoryHours: soilHours?.theoryHours ?? Number(valueOf('soilTheoryHours', '39')),
    soilLabHours: soilHours?.labHours ?? Number(valueOf('soilLabHours', '6')),
    geotechnicalTheoryHours: geotechnicalHours?.theoryHours ?? Number(valueOf('geotechnicalTheoryHours', '54')),
    geotechnicalLabHours: geotechnicalHours?.labHours ?? Number(valueOf('geotechnicalLabHours', '6')),
    labGroupSize: materialHours?.groupSize ?? Number(valueOf('labGroupSize', '25')),
    labBaseK: labK?.baseK ?? Number(valueOf('labBaseK', '0.6')),
    labBaseStudents: labK?.baseStudents ?? Number(valueOf('labBaseStudents', '25')),
    labIncrementPerStudent: labK?.incrementPerStudent ?? Number(valueOf('labIncrementPerStudent', '0.015')),
    labMinK: labK?.minK ?? Number(valueOf('labMinK', '0.5')),
    labMaxK: labK?.maxK ?? Number(valueOf('labMaxK', '1.2')),
    theoryBaseK: theoryK?.baseK ?? Number(valueOf('theoryBaseK', '1')),
    theoryBaseStudents: theoryK?.baseStudents ?? Number(valueOf('theoryBaseStudents', '40')),
    theoryIncrementPerStudent: theoryK?.incrementPerStudent ?? Number(valueOf('theoryIncrementPerStudent', '0.01')),
    theoryMinK: theoryK?.minK ?? Number(valueOf('theoryMinK', '0.9')),
    theoryMaxK: theoryK?.maxK ?? Number(valueOf('theoryMaxK', '1.5')),
    subjectRules: rules.map((rule) => ({
      code: rule.code,
      name: rule.name,
      coefficientTheoryFormula: rule.coefficientTheoryFormula,
      coefficientPracticeFormula: rule.coefficientPracticeFormula,
      formula: rule.formula,
      note: rule.note
    }))
  };
};

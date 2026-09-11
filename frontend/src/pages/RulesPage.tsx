import { CalculatorOutlined, DeleteOutlined, DownloadOutlined, EditOutlined, PlusOutlined, SearchOutlined, UploadOutlined } from '@ant-design/icons';
import { AutoComplete, Button, Card, Form, Input, Modal, Popconfirm, Select, Space, Table, Tag, Typography, message } from 'antd';
import { Fragment, type DragEvent, type ReactNode, useEffect, useMemo, useRef, useState } from 'react';
import * as XLSX from 'xlsx';
import PageHeader from '../components/PageHeader';
import { smallPagination, sttColumn } from '../constants/table';
import {
  DEFAULT_WORKLOAD_RULES,
  FORMULA_VARIABLES,
  FORMULA_VARIABLE_NOTE,
  buildCalculationSettings,
  readWorkloadRules,
  readSystemSettings,
  saveWorkloadRules,
  type WorkloadRule
} from '../constants/workloadConfig';
import {
  useRecalculateWorkloadsMutation,
  useDeleteSubjectRuleConfigMutation,
  useRuleSummariesQuery,
  useSaveSubjectRuleConfigsMutation,
  useSubjectRuleConfigsQuery,
  useTeacherWorkloadDetailsQuery
} from '../features/workloads/workloads.query';
import { antSelectFilterOption, searchMatches } from '../utils/search';

const formatNumber = (value?: number) => Number(value ?? 0).toLocaleString('vi-VN', { maximumFractionDigits: 2 });
const DELETED_RULES_STORAGE_KEY = 'klgd_deleted_subject_rules';
const FORMULA_VARIABLE_BUTTONS = [
  FORMULA_VARIABLES.students,
  FORMULA_VARIABLES.groupStudents,
  FORMULA_VARIABLES.credits,
  FORMULA_VARIABLES.weeks,
  FORMULA_VARIABLES.days,
  FORMULA_VARIABLES.advisorShare,
  FORMULA_VARIABLES.commonK,
  FORMULA_VARIABLES.theoryK,
  FORMULA_VARIABLES.practiceK
].map((variableName) => ({ label: variableName, insert: variableName }));
const MATH_BUTTONS = [
  { label: 'x²', insert: '²' },
  { label: 'x□', insert: '^()' },
  { label: 'log□', insert: 'log()' },
  { label: '√□', insert: '√()' },
  { label: '□√□', insert: 'root(,)' },
  { label: '≤', insert: '≤' },
  { label: '≥', insert: '≥' },
  { label: '÷', insert: '/' },
  { label: '×', insert: '×' },
  { label: 'π', insert: 'π' },
  { label: '+', insert: '+' },
  { label: '-', insert: '-' },
  { label: '=', insert: '=' },
  { label: '(', insert: '(' },
  { label: ')', insert: ')' },
  { label: 'min()', insert: 'min()' },
  { label: 'max()', insert: 'max()' },
  { label: '√()', insert: '√()' },
  { label: 'Σ', insert: 'Σ' },
  ...FORMULA_VARIABLE_BUTTONS
];
type RuleRow = WorkloadRule & {
  coefficientFormula: string;
  coefficientTheoryFormula: string;
  coefficientPracticeFormula: string;
  standardHoursFormula: string;
  classCount: number;
  totalStandardHours: number;
};

type RuleFormValues = WorkloadRule & {
  subjectChoice?: string;
  coefficientFormula?: string;
  coefficientTheoryFormula?: string;
  coefficientPracticeFormula?: string;
  standardHoursFormula?: string;
};

type RulesExcelRow = {
  'Môn áp dụng'?: string;
  K?: string;
  K_lt?: string;
  K_th?: string;
  'Công thức GC'?: string;
  'Công thức giờ chuẩn'?: string;
  'Ghi chú'?: string;
};

const RULE_EXCEL_HEADERS: Array<keyof RulesExcelRow> = ['Môn áp dụng', 'K', 'K_lt', 'K_th', 'Công thức GC', 'Ghi chú'];

function normalizeMathFormula(value?: string) {
  return (value ?? '')
    .replace(/\s*x\s/gi, ' × ')
    .replace(/\*/g, '×')
    .replace(/\s+\/\s+/g, '/')
    .replace(/<=/g, '≤')
    .replace(/>=/g, '≥')
    .replace(/\bmin\s*\(/gi, 'min(')
    .replace(/\bmax\s*\(/gi, 'max(')
    .replace(/\bsqrt\s*\(/gi, '√(')
    .replace(/\bpi\b/gi, 'π')
    .replace(/\balpha\b/gi, 'α')
    .replace(/\bbeta\b/gi, 'β')
    .replace(/\s+/g, ' ')
    .trim();
}

function isFractionToken(token: string) {
  return /^[\p{L}\p{N}.,()]+(\/[\p{L}\p{N}.,()]+)+$/u.test(token);
}

function renderMathToken(token: string, index: number): ReactNode {
  const punctuation = token.match(/[;,.。)]$/)?.[0] ?? '';
  const core = punctuation ? token.slice(0, -punctuation.length) : token;
  if (!isFractionToken(core)) {
    return <Fragment key={index}>{token}</Fragment>;
  }

  const parts = core.split('/');
  return (
    <Fragment key={index}>
      <span className="math-fraction" aria-label={core}>
        <span className="math-fraction-top">{parts[0]}</span>
        <span className="math-fraction-bottom">{parts.slice(1).join('/')}</span>
      </span>
      {punctuation}
    </Fragment>
  );
}

function renderMathLine(line: string) {
  const tokens = line.split(/(\s+)/);
  return tokens.map((token, index) => (token.trim() ? renderMathToken(token, index) : <Fragment key={index}>{token}</Fragment>));
}

function FormulaView({ value }: { value: string }) {
  const lines = normalizeMathFormula(value)
    .split(';')
    .map((line) => line.trim())
    .filter(Boolean);

  return (
    <div className="math-formula">
      {lines.map((line, index) => (
        <div key={`${line}-${index}`} className="math-line">
          {renderMathLine(line)}
        </div>
      ))}
    </div>
  );
}

function formulaLines(value: string) {
  return normalizeMathFormula(value)
    .split(';')
    .map((line) => line.trim())
    .filter(Boolean);
}

function combinedFormula(rule: WorkloadRule) {
  return [rule.coefficientTheoryFormula, rule.coefficientPracticeFormula, rule.formula]
    .filter(Boolean)
    .join('; ');
}

function isStandardHoursLine(line: string) {
  return /giờ chuẩn|gio chuan|\bGC\b/i.test(line);
}

function isPracticeKLine(line: string) {
  return /\bK_th\b|\bKtn\b|thực hành|thuc hanh|thí nghiệm|thi nghiem/i.test(line);
}

function isTheoryKLine(line: string) {
  return /\bK_lt\b|\bKlt\b|lý thuyết|ly thuyet/i.test(line);
}

function isGeneralKLine(line: string) {
  return /^\s*K\s*=/.test(line);
}

function splitRuleColumns(rule: WorkloadRule, classCount: number, totalStandardHours: number): RuleRow {
  const lines = formulaLines(combinedFormula(rule));
  const nonStandardLines = lines.filter((line) => !isStandardHoursLine(line));
  const storedTheory = normalizeMathFormula(rule.coefficientTheoryFormula ?? '');
  const storedPractice = normalizeMathFormula(rule.coefficientPracticeFormula ?? '');
  const generalLine = nonStandardLines.find(isGeneralKLine) || '';
  const theoryLine = storedTheory || nonStandardLines.find(isTheoryKLine) || '';
  const practiceLine = storedPractice || nonStandardLines.find(isPracticeKLine) || '';
  return {
    ...rule,
    coefficientFormula: generalLine,
    coefficientTheoryFormula: theoryLine,
    coefficientPracticeFormula: practiceLine,
    standardHoursFormula: lines.filter(isStandardHoursLine).join('; '),
    classCount,
    totalStandardHours
  };
}

function validateFormulaSyntax(value?: string) {
  const formula = (value ?? '').trim();
  if (!formula) {
    return Promise.reject(new Error('Nhập công thức'));
  }
  if (!formula.includes('=')) {
    return Promise.reject(new Error('Công thức cần có dấu ='));
  }
  const open = (formula.match(/\(/g) ?? []).length;
  const close = (formula.match(/\)/g) ?? []).length;
  if (open !== close) {
    return Promise.reject(new Error('Dấu ngoặc chưa cân bằng'));
  }
  if (/[{}[\]]/.test(formula)) {
    return Promise.reject(new Error('Không dùng ngoặc {}, []'));
  }
  return Promise.resolve();
}

function codeFromSubjectName(name: string) {
  const normalized = name
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toUpperCase()
    .replace(/[^A-Z0-9]+/g, '_')
    .replace(/^_+|_+$/g, '')
    .slice(0, 80);
  return `SUBJECT_${normalized || 'KHAC'}`;
}

function readDeletedRuleCodes() {
  try {
    return new Set<string>(JSON.parse(window.localStorage.getItem(DELETED_RULES_STORAGE_KEY) ?? '[]'));
  } catch {
    return new Set<string>();
  }
}

function saveDeletedRuleCodes(codes: Set<string>) {
  window.localStorage.setItem(DELETED_RULES_STORAGE_KEY, JSON.stringify(Array.from(codes)));
}

function normalizedText(value: string) {
  return value
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/đ/g, 'd')
    .replace(/Đ/g, 'D')
    .toLowerCase()
    .trim();
}

function defaultRuleForSubject(subjectName: string) {
  const normalizedSubject = normalizedText(subjectName);
  return DEFAULT_WORKLOAD_RULES.find((rule) => normalizedSubject.includes(normalizedText(rule.name))) ?? DEFAULT_WORKLOAD_RULES[0];
}

function mergeRules(...ruleGroups: WorkloadRule[][]) {
  const merged = new Map<string, WorkloadRule>();
  const codeByName = new Map<string, string>();
  ruleGroups.flat().forEach((rule) => {
    const ruleName = rule.name?.trim();
    const normalizedName = ruleName ? normalizedText(ruleName) : '';
    const existingCode = normalizedName ? codeByName.get(normalizedName) : undefined;
    const nextCode = existingCode || rule.code;
    if (!nextCode) {
      return;
    }
    if (normalizedName) {
      codeByName.set(normalizedName, nextCode);
    }
    merged.set(nextCode, { ...merged.get(nextCode), ...rule, code: nextCode, editable: true });
  });
  return Array.from(merged.values());
}

function ruleToSubjectRuleConfig(rule: WorkloadRule) {
  return {
    code: rule.code,
    name: rule.name,
    coefficientTheoryFormula: rule.coefficientTheoryFormula,
    coefficientPracticeFormula: rule.coefficientPracticeFormula,
    formula: rule.formula,
    note: rule.note
  };
}

function textCell(value: unknown) {
  return String(value ?? '').trim();
}

function ruleToExcelRow(rule: WorkloadRule): RulesExcelRow {
  const columns = splitRuleColumns(rule, 0, 0);
  return {
    'Môn áp dụng': rule.name,
    K: columns.coefficientFormula,
    K_lt: columns.coefficientTheoryFormula,
    K_th: columns.coefficientPracticeFormula,
    'Công thức GC': columns.standardHoursFormula,
    'Ghi chú': rule.note ?? ''
  };
}

function excelRowToRule(row: RulesExcelRow): WorkloadRule | null {
  const name = textCell(row['Môn áp dụng']);
  const code = codeFromSubjectName(name);
  const coefficientFormula = normalizeMathFormula(textCell(row.K));
  const coefficientTheoryFormula = normalizeMathFormula(textCell(row.K_lt));
  const coefficientPracticeFormula = normalizeMathFormula(textCell(row.K_th));
  const formula = normalizeMathFormula(textCell(row['Công thức GC'] || row['Công thức giờ chuẩn']));
  if (!name || !formula) {
    return null;
  }
  return {
    code,
    name,
    coefficientTheoryFormula,
    coefficientPracticeFormula,
    formula: [coefficientFormula, formula].filter(Boolean).join('; '),
    note: textCell(row['Ghi chú']),
    editable: true
  };
}

function normalizeImportedRules(rows: RulesExcelRow[]) {
  return rows
    .map(excelRowToRule)
    .filter((rule): rule is WorkloadRule => Boolean(rule));
}

function buildRulesWorkbook(rows: RulesExcelRow[], includeGuide = false) {
  const workbook = XLSX.utils.book_new();
  const worksheet = XLSX.utils.json_to_sheet(rows, { header: RULE_EXCEL_HEADERS });
  worksheet['!cols'] = [
    { wch: 36 },
    { wch: 48 },
    { wch: 48 },
    { wch: 48 },
    { wch: 48 },
    { wch: 56 }
  ];
  XLSX.utils.book_append_sheet(workbook, worksheet, 'Quy tắc');

  if (includeGuide) {
    const guide = XLSX.utils.aoa_to_sheet([
      ['Hướng dẫn import quy tắc tính'],
      ['Môn áp dụng', 'Bắt buộc. Nhập đúng tên môn/học phần muốn áp dụng.'],
      ['K', 'Không bắt buộc. Dùng cho hệ số chung. Ví dụ: K = min(max(1.0 + (SV - 40) × 0.01, 0.9), 1.5)'],
      ['K_lt', 'Không bắt buộc. Ví dụ: K_lt = min(max(1.0 + (SV - 40) × 0.01, 0.9), 1.5)'],
      ['K_th', 'Không bắt buộc. Ví dụ: K_th = max(0.6 + (SV - 25) × 0.015, 0.5)'],
      ['Công thức GC', 'Bắt buộc. Ví dụ: GC = 42 × K_lt + 9 × K_th'],
      ['Ghi chú', 'Không bắt buộc.'],
      [],
      ['Quy ước biến trong công thức'],
      ['SV', 'Số sinh viên của lớp/học phần.'],
      ['SV_nhom', 'Số sinh viên trong một nhóm thí nghiệm/thực hành.'],
      ['TC', 'Tín chỉ.'],
      ['K', 'Hệ số K chung.'],
      ['K_lt', 'Hệ số phần lý thuyết.'],
      ['K_th', 'Hệ số phần thực hành/thí nghiệm.'],
      [],
      ['Hàm và phép toán hợp lệ'],
      ['min(a, b)', 'Lấy giá trị nhỏ hơn.'],
      ['max(a, b)', 'Lấy giá trị lớn hơn.'],
      ['√(a)', 'Căn bậc hai. Có thể nhập sqrt(a), hệ thống sẽ đổi thành √(a).'],
      ['root(a, b)', 'Căn bậc b của a.'],
      ['+, -, ×, /', 'Cộng, trừ, nhân, chia. Có thể nhập * thay cho ×.'],
      ['≤, ≥', 'So sánh nhỏ hơn/bằng, lớn hơn/bằng. Có thể nhập <= hoặc >=.'],
      ['π', 'Số pi. Có thể nhập pi.'],
      [],
      ['Lưu ý'],
      ['Không tự đặt biến mới', 'Nếu dùng chữ viết tắt khác ngoài danh sách trên, hệ thống không hiểu đúng công thức.'],
      ['Dấu ngoặc', 'Các hàm như min(), max(), √() phải có đủ mở và đóng ngoặc.'],
      ['Công thức GC', 'Nên bắt đầu bằng "GC = ..." để hệ thống nhận diện cột GC.']
    ]);
    guide['!cols'] = [{ wch: 24 }, { wch: 90 }];
    XLSX.utils.book_append_sheet(workbook, guide, 'Hướng dẫn');
  }

  return workbook;
}

export default function RulesPage() {
  const summaries = useRuleSummariesQuery();
  const details = useTeacherWorkloadDetailsQuery();
  const subjectRules = useSubjectRuleConfigsQuery();
  const saveSubjectRules = useSaveSubjectRuleConfigsMutation();
  const deleteSubjectRule = useDeleteSubjectRuleConfigMutation();
  const recalculateWorkloads = useRecalculateWorkloadsMutation();
  const [rules, setRules] = useState<WorkloadRule[]>(() => readWorkloadRules());
  const [keyword, setKeyword] = useState('');
  const [formulaFilter, setFormulaFilter] = useState<string>();
  const [usageFilter, setUsageFilter] = useState<string>();
  const [editingRule, setEditingRule] = useState<WorkloadRule | null>(null);
  const [showMathKeyboard, setShowMathKeyboard] = useState(false);
  const [activeFormulaField, setActiveFormulaField] = useState<'coefficientFormula' | 'coefficientTheoryFormula' | 'coefficientPracticeFormula' | 'standardHoursFormula'>('coefficientFormula');
  const [subjectSearch, setSubjectSearch] = useState('');
  const [importModalOpen, setImportModalOpen] = useState(false);
  const autoSyncedMissingSubjects = useRef(false);
  const importInputRef = useRef<HTMLInputElement>(null);
  const [form] = Form.useForm<RuleFormValues>();
  const summaryMap = new Map((summaries.data ?? []).map((item) => [item.ruleCode, item]));

  useEffect(() => {
    if (subjectRules.data) {
      const backendRules = subjectRules.data.map((rule) => ({
        code: rule.code,
        name: rule.name,
        coefficientTheoryFormula: rule.coefficientTheoryFormula,
        coefficientPracticeFormula: rule.coefficientPracticeFormula,
        formula: rule.formula,
        note: rule.note ?? '',
        editable: true
      }));
      const deletedCodes = readDeletedRuleCodes();
      const storedRules = readWorkloadRules().filter((rule) => !deletedCodes.has(rule.code));
      const activeBackendRules = backendRules.filter((rule) => !deletedCodes.has(rule.code));
      const nextRules = activeBackendRules.length
        ? mergeRules(storedRules, activeBackendRules)
        : mergeRules(DEFAULT_WORKLOAD_RULES, storedRules);
      setRules(nextRules);
      saveWorkloadRules(nextRules);
    }
  }, [subjectRules.data]);

  useEffect(() => {
    if (autoSyncedMissingSubjects.current || subjectRules.isLoading || !details.data?.length) {
      return;
    }
    const importedSubjects = Array.from(new Set(details.data.map((item) => item.subjectName).filter(Boolean)));
    const storedRules = subjectRules.data?.length
      ? subjectRules.data.map((rule) => ({
        code: rule.code,
        name: rule.name,
        coefficientTheoryFormula: rule.coefficientTheoryFormula,
        coefficientPracticeFormula: rule.coefficientPracticeFormula,
        formula: rule.formula,
        note: rule.note ?? '',
        editable: true
      }))
      : rules;
    const deletedCodes = readDeletedRuleCodes();
    const existingNames = new Set(storedRules.map((rule) => rule.name.trim().toLowerCase()));
    const missingRules = importedSubjects
      .filter((subjectName) => !existingNames.has(subjectName.trim().toLowerCase()))
      .filter((subjectName) => !deletedCodes.has(codeFromSubjectName(subjectName)))
      .map((subjectName) => {
        const template = defaultRuleForSubject(subjectName);
        return {
          code: codeFromSubjectName(subjectName),
          name: subjectName,
          coefficientTheoryFormula: template.coefficientTheoryFormula,
          coefficientPracticeFormula: template.coefficientPracticeFormula,
          formula: template.formula,
          note: `Tự tạo từ dữ liệu import theo mẫu "${template.name}", có thể sửa công thức riêng cho môn này.`,
          editable: true
        };
      });
    if (missingRules.length === 0) {
      return;
    }
    autoSyncedMissingSubjects.current = true;
    const nextRules = [...storedRules, ...missingRules];
    setRules(nextRules);
    saveWorkloadRules(nextRules);
    void saveSubjectRules.mutateAsync(nextRules.map(ruleToSubjectRuleConfig))
      .then(() => recalculateWorkloads.mutateAsync(buildCalculationSettings(readSystemSettings())));
  }, [details.data, recalculateWorkloads, rules, saveSubjectRules, subjectRules.data, subjectRules.isLoading]);
  const subjectOptions = useMemo(() => {
    const names = Array.from(new Set((details.data ?? []).map((item) => item.subjectName).filter(Boolean)))
      .sort((left, right) => left.localeCompare(right, 'vi'));
    const ruledNames = new Set(rules.map((rule) => normalizedText(rule.name)));
    const withoutRules = names
      .filter((name) => !ruledNames.has(normalizedText(name)))
      .map((name) => ({ label: name, value: name }));
    const withRules = rules
      .map((rule) => rule.name)
      .filter(Boolean)
      .sort((left, right) => left.localeCompare(right, 'vi'))
      .map((name) => ({ label: name, value: name, disabled: !editingRule?.code }));
    return [
      {
        label: withoutRules.length ? 'Môn chưa có quy tắc' : 'Tất cả môn đã có quy tắc',
        options: withoutRules
      },
      {
        label: 'Môn đã có quy tắc',
        options: withRules
      }
    ];
  }, [details.data, editingRule?.code, rules]);

  const subjectSelectOptions = useMemo(() => {
    const keyword = subjectSearch.trim();
    const flatOptions = subjectOptions.flatMap((group) => group.options);
    const hasExactSubject = flatOptions.some((option) => normalizedText(option.value) === normalizedText(keyword));
    if (!keyword || hasExactSubject) {
      return subjectOptions;
    }
    return [
      ...subjectOptions,
      {
        label: 'Tạo mới',
        options: [{ label: `Tạo môn mới: ${keyword}`, value: keyword }]
      }
    ];
  }, [subjectOptions, subjectSearch]);

  const rows = useMemo<RuleRow[]>(() => rules.map((rule) => {
    const summary = summaryMap.get(rule.code);
    return splitRuleColumns(rule, summary?.classCount ?? 0, summary?.totalStandardHours ?? 0);
  }), [rules, summaryMap]);

  const visibleRows = useMemo(() => {
    return rows.filter((rule) => {
      const matchKeyword = !keyword.trim() || searchMatches(`${rule.code} ${rule.name} ${rule.coefficientFormula} ${rule.coefficientTheoryFormula} ${rule.coefficientPracticeFormula} ${rule.standardHoursFormula} ${rule.note}`, keyword);
      const matchFormula = !formulaFilter
        || (formulaFilter === 'hasCoefficient' && (Boolean(rule.coefficientFormula) || Boolean(rule.coefficientTheoryFormula) || Boolean(rule.coefficientPracticeFormula)))
        || (formulaFilter === 'missingCoefficient' && !rule.coefficientFormula && !rule.coefficientTheoryFormula && !rule.coefficientPracticeFormula)
        || (formulaFilter === 'hasStandardHours' && Boolean(rule.standardHoursFormula))
        || (formulaFilter === 'missingStandardHours' && !rule.standardHoursFormula);
      const matchUsage = !usageFilter
        || (usageFilter === 'used' && rule.classCount > 0)
        || (usageFilter === 'unused' && rule.classCount === 0);
      return matchKeyword && matchFormula && matchUsage;
    });
  }, [formulaFilter, keyword, rows, usageFilter]);

  const openEdit = (rule: WorkloadRule) => {
    const columns = splitRuleColumns(rule, 0, 0);
    setEditingRule(rule);
    setShowMathKeyboard(false);
    setSubjectSearch('');
    form.setFieldsValue({
      ...rule,
      subjectChoice: rule.name,
      coefficientFormula: columns.coefficientFormula,
      coefficientTheoryFormula: columns.coefficientTheoryFormula,
      coefficientPracticeFormula: columns.coefficientPracticeFormula,
      standardHoursFormula: columns.standardHoursFormula,
      formula: columns.standardHoursFormula
    });
  };

  const openCreate = () => {
    setEditingRule({
      code: '',
      name: '',
      formula: '',
      note: '',
      editable: true
    });
    setShowMathKeyboard(false);
    setSubjectSearch('');
    form.resetFields();
    form.setFieldsValue({
      subjectChoice: undefined,
      coefficientFormula: `${FORMULA_VARIABLES.commonK} = min(max(1.0 + (${FORMULA_VARIABLES.students} - 40) × 0.01, 0.9), 1.5)`,
      coefficientTheoryFormula: '',
      coefficientPracticeFormula: '',
      standardHoursFormula: `${FORMULA_VARIABLES.standardHours} = ${FORMULA_VARIABLES.credits} × 15 × ${FORMULA_VARIABLES.commonK}`,
      note: 'Quy tắc tự tạo, áp dụng cho môn/học phần đã chọn.'
    });
  };

  const normalizeCurrentFormula = () => {
    form.setFieldValue('coefficientFormula', normalizeMathFormula(form.getFieldValue('coefficientFormula')));
    form.setFieldValue('coefficientTheoryFormula', normalizeMathFormula(form.getFieldValue('coefficientTheoryFormula')));
    form.setFieldValue('coefficientPracticeFormula', normalizeMathFormula(form.getFieldValue('coefficientPracticeFormula')));
    form.setFieldValue('standardHoursFormula', normalizeMathFormula(form.getFieldValue('standardHoursFormula')));
  };

  const insertMathToken = (token: string) => {
    const current = form.getFieldValue(activeFormulaField) ?? '';
    const spacer = current && !current.endsWith(' ') && !/^[),^]/.test(token) ? ' ' : '';
    form.setFieldValue(activeFormulaField, `${current}${spacer}${token}`);
  };

  const insertFractionToken = () => {
    const current = form.getFieldValue(activeFormulaField) ?? '';
    const spacer = current && !current.endsWith(' ') ? ' ' : '';
    form.setFieldValue(activeFormulaField, `${current}${spacer}()/()`);
  };

  const saveRule = async () => {
    const values = await form.validateFields();
    const nextName = values.subjectChoice?.trim();
    const nextCode = editingRule?.code || codeFromSubjectName(nextName || values.name || 'KHAC');
    const coefficientFormula = normalizeMathFormula(values.coefficientFormula);
    const coefficientTheoryFormula = normalizeMathFormula(values.coefficientTheoryFormula);
    const coefficientPracticeFormula = normalizeMathFormula(values.coefficientPracticeFormula);
    const formula = [coefficientFormula, normalizeMathFormula(values.standardHoursFormula)].filter(Boolean).join('; ');
    const normalizedValues = {
      ...values,
      code: nextCode,
      name: nextName || values.name || '',
      coefficientTheoryFormula,
      coefficientPracticeFormula,
      formula,
      editable: true
    };
    const existingRule = rules.some((item) => item.code === nextCode);
    const nextRules = existingRule
      ? rules.map((item) => (item.code === nextCode ? { ...item, ...normalizedValues } : item))
      : [...rules, normalizedValues];
    const deletedCodes = readDeletedRuleCodes();
    deletedCodes.delete(nextCode);
    saveDeletedRuleCodes(deletedCodes);
    setRules(nextRules);
    saveWorkloadRules(nextRules);
    await saveSubjectRules.mutateAsync(nextRules.map(ruleToSubjectRuleConfig));
    await recalculateWorkloads.mutateAsync(buildCalculationSettings(readSystemSettings()));
    setEditingRule(null);
    message.success('Đã lưu quy tắc và tính lại dữ liệu');
  };

  const deleteRule = async (rule: WorkloadRule) => {
    const nextRules = rules.filter((item) => item.code !== rule.code);
    const deletedCodes = readDeletedRuleCodes();
    deletedCodes.add(rule.code);
    saveDeletedRuleCodes(deletedCodes);
    setRules(nextRules);
    saveWorkloadRules(nextRules);
    await deleteSubjectRule.mutateAsync(rule.code);
    await recalculateWorkloads.mutateAsync(buildCalculationSettings(readSystemSettings()));
    message.success('Đã xóa quy tắc và tính lại dữ liệu');
  };

  const exportRules = () => {
    const workbook = buildRulesWorkbook(rules.map(ruleToExcelRow));
    XLSX.writeFile(workbook, `quy-tac-tinh-${new Date().toISOString().slice(0, 10)}.xlsx`);
  };

  const exportRulesTemplate = () => {
    const workbook = buildRulesWorkbook([], true);
    XLSX.writeFile(workbook, 'mau-import-quy-tac-tinh.xlsx');
  };

  const importRules = async (file?: File) => {
    if (!file) {
      return;
    }
    try {
      const workbook = XLSX.read(await file.arrayBuffer(), { type: 'array' });
      const sheet = workbook.Sheets['Quy tắc'] ?? workbook.Sheets[workbook.SheetNames[0]];
      const rows = XLSX.utils.sheet_to_json<RulesExcelRow>(sheet, { defval: '' });
      const imported = normalizeImportedRules(rows);
      if (imported.length === 0) {
        message.error('File không có quy tắc hợp lệ');
        return;
      }
      const nextRules = mergeRules(rules, imported);
      const deletedCodes = readDeletedRuleCodes();
      imported.forEach((rule) => deletedCodes.delete(rule.code));
      saveDeletedRuleCodes(deletedCodes);
      setRules(nextRules);
      saveWorkloadRules(nextRules);
      await saveSubjectRules.mutateAsync(nextRules.map(ruleToSubjectRuleConfig));
      await recalculateWorkloads.mutateAsync(buildCalculationSettings(readSystemSettings()));
      message.success(`Đã import ${imported.length} quy tắc và tính lại dữ liệu`);
      setImportModalOpen(false);
    } catch {
      message.error('Không đọc được file quy tắc');
    } finally {
      if (importInputRef.current) {
        importInputRef.current.value = '';
      }
    }
  };

  const handleImportDrop = (event: DragEvent<HTMLDivElement>) => {
    event.preventDefault();
    void importRules(event.dataTransfer.files?.[0]);
  };

  return (
    <div className="page-stack rules-page">
      <PageHeader title="Quy tắc tính" />

      <Card className="formula-note-card">
        <Typography.Text strong>Bảng ký hiệu công thức</Typography.Text>
        <Typography.Paragraph className="formula-note-text">
          {FORMULA_VARIABLE_NOTE}
        </Typography.Paragraph>
      </Card>

      <Card title="Danh sách quy tắc">
        <Space className="rules-filter-toolbar">
          <Input
            allowClear
            prefix={<SearchOutlined />}
            placeholder="Tìm theo tên môn, phần tính, mã quy tắc hoặc công thức"
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
            className="rules-search-input"
          />
          <Select
            allowClear
            placeholder="Lọc công thức"
            value={formulaFilter}
            onChange={setFormulaFilter}
            className="filter-select rules-filter-select"
            options={[
              { label: 'Có hệ số K', value: 'hasCoefficient' },
              { label: 'Chưa có hệ số K', value: 'missingCoefficient' },
              { label: 'Có GC', value: 'hasStandardHours' },
              { label: 'Chưa có GC', value: 'missingStandardHours' }
            ]}
          />
          <Select
            allowClear
            placeholder="Lọc sử dụng"
            value={usageFilter}
            onChange={setUsageFilter}
            className="filter-select rules-filter-select"
            options={[
              { label: 'Đã tính lớp', value: 'used' },
              { label: 'Chưa tính lớp', value: 'unused' }
            ]}
          />
          <Button onClick={() => {
            setKeyword('');
            setFormulaFilter(undefined);
            setUsageFilter(undefined);
          }}>
            Xóa lọc
          </Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
            Thêm quy tắc
          </Button>
          <Button icon={<DownloadOutlined />} onClick={exportRules}>
            Xuất Excel
          </Button>
          <Button icon={<UploadOutlined />} onClick={() => setImportModalOpen(true)}>
            Import Excel
          </Button>
          <input
            ref={importInputRef}
            type="file"
            accept=".xlsx,.xls,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet,application/vnd.ms-excel"
            hidden
            onChange={(event) => void importRules(event.target.files?.[0])}
          />
        </Space>
        <Table
          className="rules-table"
          rowKey="code"
          tableLayout="fixed"
          scroll={{ x: 2400 }}
          dataSource={visibleRows}
          loading={summaries.isLoading || subjectRules.isLoading || saveSubjectRules.isPending}
          pagination={smallPagination}
          size="small"
          columns={[
            sttColumn,
            {
              title: 'Môn áp dụng',
              dataIndex: 'name',
              width: 380,
              className: 'text-left',
              render: (value: string, record) => (
                <Space direction="vertical" size={4} className="rule-name-stack">
                  <Typography.Text strong>{value}</Typography.Text>
                  <Tag color="cyan">{record.code}</Tag>
                </Space>
              )
            },
            {
              title: 'K',
              dataIndex: 'coefficientFormula',
              width: 360,
              className: 'text-left',
              render: (value: string) => value ? <FormulaView value={value} /> : <span className="empty-note-inline">Không cấu hình</span>
            },
            {
              title: 'K_lt',
              dataIndex: 'coefficientTheoryFormula',
              width: 360,
              className: 'text-left',
              render: (value: string) => value ? <FormulaView value={value} /> : <span className="empty-note-inline">Không cấu hình</span>
            },
            {
              title: 'K_th',
              dataIndex: 'coefficientPracticeFormula',
              width: 360,
              className: 'text-left',
              render: (value: string) => value ? <FormulaView value={value} /> : <span className="empty-note-inline">Không cấu hình</span>
            },
            {
              title: 'GC',
              dataIndex: 'standardHoursFormula',
              width: 430,
              className: 'text-left',
              render: (value: string) => value ? <FormulaView value={value} /> : <span className="empty-note-inline">Chưa có công thức</span>
            },
            { title: 'Ghi chú', dataIndex: 'note', width: 360, className: 'text-left' },
            { title: 'Số lớp đã tính', dataIndex: 'classCount', width: 140 },
            { title: 'GC đã tính', dataIndex: 'totalStandardHours', width: 140, render: formatNumber },
            {
              title: 'Thao tác',
              width: 180,
              render: (_, record) => (
                <Space>
                  <Button size="small" icon={<EditOutlined />} onClick={() => openEdit(record)}>
                    Sửa
                  </Button>
                  <Popconfirm
                    title="Xóa quy tắc này?"
                    description="Môn này sẽ quay về quy tắc mặc định nếu không còn quy tắc riêng."
                    okText="Xóa"
                    cancelText="Hủy"
                    onConfirm={() => deleteRule(record)}
                  >
                    <Button size="small" danger icon={<DeleteOutlined />} loading={deleteSubjectRule.isPending}>
                      Xóa
                    </Button>
                  </Popconfirm>
                </Space>
              )
            }
          ]}
        />
      </Card>

      <Modal
        title="Import quy tắc tính từ Excel"
        open={importModalOpen}
        onCancel={() => setImportModalOpen(false)}
        footer={null}
        destroyOnClose
      >
        <div
          className="rules-import-dropzone"
          onDragOver={(event) => event.preventDefault()}
          onDrop={handleImportDrop}
        >
          <UploadOutlined />
          <Typography.Text className="rules-import-title">Kéo thả hoặc chọn file quy tắc tính</Typography.Text>
          <Typography.Text type="secondary">Chỉ nhận file .xlsx/.xls theo đúng file mẫu.</Typography.Text>
          <Space wrap>
            <Button icon={<DownloadOutlined />} onClick={exportRulesTemplate}>
              Tải file mẫu
            </Button>
            <Button type="primary" icon={<UploadOutlined />} onClick={() => importInputRef.current?.click()}>
              Chọn file Excel
            </Button>
          </Space>
        </div>
      </Modal>

      <Modal
        title={editingRule?.code ? 'Sửa quy tắc' : 'Thêm quy tắc'}
        open={Boolean(editingRule)}
        onCancel={() => setEditingRule(null)}
        onOk={saveRule}
        okText="Lưu"
        cancelText="Hủy"
        destroyOnClose
      >
        <Form form={form} layout="vertical">
          <Form.Item name="subjectChoice" label="Môn/học phần áp dụng" rules={[{ required: true, message: 'Chọn môn/học phần áp dụng' }]}>
            <AutoComplete
              options={subjectSelectOptions}
              filterOption={antSelectFilterOption}
              onSearch={setSubjectSearch}
              onChange={(value) => {
                form.setFieldValue('subjectChoice', value);
                setSubjectSearch(value);
              }}
              popupClassName="subject-rule-picker-popup"
              popupMatchSelectWidth={false}
              placeholder="Chọn môn có sẵn hoặc gõ tên môn mới"
            />
          </Form.Item>
          <Form.Item
            name="coefficientFormula"
            label="K"
            rules={[
              { validator: (_, value) => (value ? validateFormulaSyntax(value) : Promise.resolve()) }
            ]}
          >
            <Input.TextArea rows={2} onFocus={() => setActiveFormulaField('coefficientFormula')} onBlur={normalizeCurrentFormula} />
          </Form.Item>
          <Form.Item
            name="coefficientTheoryFormula"
            label="K_lt"
            rules={[
              { validator: (_, value) => (value ? validateFormulaSyntax(value) : Promise.resolve()) }
            ]}
          >
            <Input.TextArea rows={2} onFocus={() => setActiveFormulaField('coefficientTheoryFormula')} onBlur={normalizeCurrentFormula} />
          </Form.Item>
          <Form.Item
            name="coefficientPracticeFormula"
            label="K_th"
            rules={[
              { validator: (_, value) => (value ? validateFormulaSyntax(value) : Promise.resolve()) }
            ]}
          >
            <Input.TextArea rows={2} onFocus={() => setActiveFormulaField('coefficientPracticeFormula')} onBlur={normalizeCurrentFormula} />
          </Form.Item>
          <Form.Item
            name="standardHoursFormula"
            label="Công thức GC"
            rules={[
              { required: true, message: 'Nhập công thức GC' },
              { validator: (_, value) => validateFormulaSyntax(value) }
            ]}
          >
            <Input.TextArea rows={3} onFocus={() => setActiveFormulaField('standardHoursFormula')} onBlur={normalizeCurrentFormula} />
          </Form.Item>
          <Button icon={<CalculatorOutlined />} onClick={() => setShowMathKeyboard((value) => !value)} className="math-toggle-button">
            {showMathKeyboard ? 'Ẩn bàn phím toán học' : 'Mở bàn phím toán học'}
          </Button>
          {showMathKeyboard && (
            <div className="math-editor-panel math-editor-panel-under">
              <div className="math-editor-header">
                <CalculatorOutlined />
                <span>Bàn phím ký hiệu toán học</span>
              </div>
              <div className="math-symbol-grid">
                <Button size="small" className="fraction-button" onClick={insertFractionToken}>
                  <span />
                </Button>
                {MATH_BUTTONS.map((button) => (
                  <Button key={`${button.label}-${button.insert}`} size="small" onClick={() => insertMathToken(button.insert)}>
                    {button.label}
                  </Button>
                ))}
              </div>
              <div className="math-variable-help">
                <span><strong>{FORMULA_VARIABLES.students}</strong>: số sinh viên.</span>
                <span><strong>{FORMULA_VARIABLES.groupStudents}</strong>: số sinh viên trong nhóm thí nghiệm.</span>
                <span><strong>{FORMULA_VARIABLES.credits}</strong>: tín chỉ.</span>
                <span><strong>{FORMULA_VARIABLES.standardHours}</strong>: giờ chuẩn.</span>
                <span><strong>{FORMULA_VARIABLES.commonK}</strong>: hệ số chung.</span>
                <span><strong>{FORMULA_VARIABLES.weeks}/{FORMULA_VARIABLES.days}</strong>: số tuần/số ngày.</span>
                <span><strong>{FORMULA_VARIABLES.advisorShare}</strong>: hệ số hướng dẫn.</span>
                <span><strong>K_lt/K_th</strong>: hệ số từng phần.</span>
              </div>
            </div>
          )}
          <Form.Item name="note" label="Ghi chú">
            <Input.TextArea rows={3} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}

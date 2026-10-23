package com.discount.discount_web.service;

import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 📅 從優惠文字抽取「結束日期」與「優惠期顯示文字」
 * 原則：抽到先填，抽唔到回傳 null 交返管理員喺待審時人手補，絕不猜測。
 */
public class DateExtractor {

    public static class Result {
        public final LocalDate endDate;
        public final String periodText;
        public Result(LocalDate endDate, String periodText) {
            this.endDate = endDate;
            this.periodText = periodText;
        }
    }

    // 例：2026年10月16日至11月12日 / 2026年10月16日至2026年11月12日
    private static final Pattern CN_RANGE = Pattern.compile(
            "(\\d{4})\\s*年\\s*(\\d{1,2})\\s*月\\s*(\\d{1,2})\\s*日\\s*至\\s*(?:(\\d{4})\\s*年\\s*)?(\\d{1,2})\\s*月\\s*(\\d{1,2})\\s*日");

    // 例：即日起至 2026/11/12 或 至2026-11-12
    private static final Pattern UNTIL_SLASH = Pattern.compile(
            "至\\s*(\\d{4})\\s*[-/](\\d{1,2})\\s*[-/](\\d{1,2})");

    // 例：即日起至 12/11/2026 (DD/MM/YYYY)
    private static final Pattern UNTIL_DMY = Pattern.compile(
            "至\\s*(\\d{1,2})\\s*/\\s*(\\d{1,2})\\s*/\\s*(\\d{4})");

    // 例：優惠期至10月31日 / 10月31日止 (冇年份，用今年；已過咗超過 60 日當下一年)
    private static final Pattern CN_UNTIL_NO_YEAR = Pattern.compile(
            "(?:至|到)\\s*(\\d{1,2})\\s*月\\s*(\\d{1,2})\\s*日");

    public static Result extract(String text) {
        if (text == null || text.isBlank()) {
            return new Result(null, null);
        }

        Matcher m = CN_RANGE.matcher(text);
        if (m.find()) {
            int startYear = Integer.parseInt(m.group(1));
            int endYear = m.group(4) != null ? Integer.parseInt(m.group(4)) : startYear;
            int endMonth = Integer.parseInt(m.group(5));
            int endDay = Integer.parseInt(m.group(6));
            // 跨年活動 (例：12月至1月) 而冇寫結束年份 → 結束年份 +1
            int startMonth = Integer.parseInt(m.group(2));
            if (m.group(4) == null && endMonth < startMonth) {
                endYear = startYear + 1;
            }
            LocalDate d = safeDate(endYear, endMonth, endDay);
            if (d != null) return new Result(d, m.group(0));
        }

        m = UNTIL_SLASH.matcher(text);
        if (m.find()) {
            LocalDate d = safeDate(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)));
            if (d != null) return new Result(d, m.group(0));
        }

        m = UNTIL_DMY.matcher(text);
        if (m.find()) {
            LocalDate d = safeDate(Integer.parseInt(m.group(3)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(1)));
            if (d != null) return new Result(d, m.group(0));
        }

        m = CN_UNTIL_NO_YEAR.matcher(text);
        if (m.find()) {
            int month = Integer.parseInt(m.group(1));
            int day = Integer.parseInt(m.group(2));
            LocalDate now = LocalDate.now();
            LocalDate d = safeDate(now.getYear(), month, day);
            if (d != null && d.isBefore(now.minusDays(60))) {
                d = safeDate(now.getYear() + 1, month, day);
            }
            if (d != null) return new Result(d, m.group(0));
        }

        return new Result(null, null);
    }

    private static LocalDate safeDate(int y, int mo, int d) {
        try {
            return LocalDate.of(y, mo, d);
        } catch (Exception e) {
            return null;
        }
    }
}

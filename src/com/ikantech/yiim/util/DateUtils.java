package com.ikantech.yiim.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import android.content.Context;

import com.ikantech.yiim.R;

public class DateUtils {
	public static String format(Context context, Date date) {
		Calendar now = Calendar.getInstance();
		Calendar target = Calendar.getInstance();
		target.setTime(date);

		StringBuilder result = new StringBuilder();
		String suffix;
		if (now.after(target)) {
			suffix = context.getString(R.string.str_before);
		} else {
			suffix = context.getString(R.string.str_after);
		}

		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm");

		int day_sub = Math.abs(now.get(Calendar.DAY_OF_MONTH)
				- target.get(Calendar.DAY_OF_MONTH));

		if (now.get(Calendar.YEAR) == target.get(Calendar.YEAR)) {// 如果年份一样
			if (now.get(Calendar.MONTH) == target.get(Calendar.MONTH)) {// 如果月份一样
				if (day_sub == 0) {// 如果是同一天
					if (Math.abs(now.get(Calendar.HOUR_OF_DAY)
							- target.get(Calendar.HOUR_OF_DAY)) <= 1) {// 如果是一小时内
						// 显示n分钟前(后)
						int minute = Math.abs(target.get(Calendar.MINUTE)
								- now.get(Calendar.MINUTE));
						if (minute != 0) {
							result.append(minute + "");
							result.append(context
									.getString(R.string.str_minute));
						} else {
							// 显示n秒前(后)
							int second = Math.abs(target.get(Calendar.SECOND)
									- now.get(Calendar.SECOND));
							result.append(second + "");
							result.append(context
									.getString(R.string.str_second));
						}
					} else {
						// 日期相同显示小时 “n小时前(后)”
						int hour = Math.abs(target.get(Calendar.HOUR_OF_DAY)
								- now.get(Calendar.HOUR_OF_DAY));
						result.append(hour + "");
						result.append(context.getString(R.string.str_hour));
					}
					result.append(suffix);
				} else if (day_sub >= 1 && day_sub <= 2) {
					int now_day = now.get(Calendar.DAY_OF_MONTH);
					int date_day = target.get(Calendar.DAY_OF_MONTH);
					switch (now_day - date_day) {
					case 1:
						result.append(context.getString(R.string.str_yesterday));
						break;
					case -1:
						result.append(context.getString(R.string.str_tomorrow));
						break;
					case 2:
						result.append(context
								.getString(R.string.str_day_before_yesterday));
						break;
					case -2:
						result.append(context
								.getString(R.string.str_day_after_tomorrow));
						break;
					default:
						break;
					}
					result.append(simpleDateFormat.format(target.getTime()));
				} else {
					// 月份相同，显示“n天前(后)"
					int day = Math.abs(target.get(Calendar.DAY_OF_MONTH)
							- now.get(Calendar.DAY_OF_MONTH));
					result.append(day + "");
					result.append(context.getString(R.string.str_day));
					result.append(suffix);

					result.append(simpleDateFormat.format(target.getTime()));
				}
			} else {
				// 年份相同，显示日期“2月3日”
				int month = target.get(Calendar.MONTH) + 1;
				int day = target.get(Calendar.DAY_OF_MONTH);
				result.append(month + context.getString(R.string.str_month));
				result.append(day + context.getString(R.string.str_date_day));
			}
		} else {
			// 显示年份+日期 “2012-01-02”
			int year = target.get(Calendar.YEAR);
			int month = target.get(Calendar.MONTH) + 1;
			int day = target.get(Calendar.DAY_OF_MONTH);
			result.append(String.format("%04d-%02d-%02d", year, month, day));
		}
		return result.toString();
	}
}

package eliseev.aiadvent.chat.presentation.chat.mapper

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object DateFormatHelper {
    
    fun createIso8601Format(locale: Locale = Locale.US): SimpleDateFormat {
        return SimpleDateFormat(ISO_8601_WITH_MILLIS_PATTERN, locale).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }
    
    fun createIso8601FormatWithoutMillis(locale: Locale = Locale.US): SimpleDateFormat {
        return SimpleDateFormat(ISO_8601_WITHOUT_MILLIS_PATTERN, locale).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }
    
    fun createOutputFormat(locale: Locale = Locale("ru")): SimpleDateFormat {
        return SimpleDateFormat(OUTPUT_DATE_PATTERN, locale)
    }
    
    private const val ISO_8601_WITH_MILLIS_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
    private const val ISO_8601_WITHOUT_MILLIS_PATTERN = "yyyy-MM-dd'T'HH:mm:ss'Z'"
    private const val OUTPUT_DATE_PATTERN = "d MMMM yyyy HH:mm"
}

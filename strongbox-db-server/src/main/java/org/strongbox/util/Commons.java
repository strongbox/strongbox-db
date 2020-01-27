package org.strongbox.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

public class Commons
{

    public static LocalDateTime toLocalDateTime(Date date)
    {
        if (date == null)
        {
            return null;
        }
        return date.toInstant()
                   .atZone(ZoneId.systemDefault())
                   .toLocalDateTime();
    }

    public static Date toDate(LocalDateTime date)
    {
        if (date == null)
        {
            return null;
        }
        return Date.from(date.atZone(ZoneId.systemDefault()).toInstant());
    }
    
    public static LocalDateTime toLocalDateTime(Long value)
    {
        if (value == null)
        {
            return null;
        }
        return Instant.ofEpochMilli(value).atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    public static Long toLong(LocalDateTime date)
    {
        if (date == null)
        {
            return null;
        }
        return date.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
    
}

// $Header: /home/cvs/jakarta-jmeter/src/core/org/apache/jmeter/gui/util/JDateFieldAlt.java,v 1.8 2004/02/13 02:21:38 sebb Exp $
/*
 * Copyright 2003-2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
*/

package com.moneydance.modules.features.invextension;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.swing.JTextField;

/**
 * This is Date mask control. Using this control we can pop up our date in the
 * text field. And this control is Devloped basically for  JDK1.3 and lower
 * version support. This control is similer to JSpinner control this is
 * available in JDK1.4 and above only.
 * <p>
 * This will set the date "yyyy/MM/dd HH:mm:ss" in this format only.
 * </p>
 *
 * @author    T.Elanjchezhiyan
 * @version   $Revision: 1.8 $ Last update: $Date: 2004/02/13 02:21:38 $
 */
public class JDateFieldAlt extends JTextField
{
    private final static DateFormat dateFormat =
        new SimpleDateFormat("MM/dd/yyyy"); //"yyyy/MM/dd HH:mm:ss"

    /*
     * The following array must agree with dateFormat
     *
     * It is used to translate the positions in the buffer
     * to the values used by the Calendar class for the field id.
     *
     * Current format:
     * MM/DD/YYYY HH:MM:SS
     * 01234567890123456789
     * ^buffer positions
     */
    private static int fieldPositions [] = {
        
        
        Calendar.MONTH,        // M
        Calendar.MONTH,        // M
        Calendar.MONTH,        // /
        Calendar.YEAR,         // Y
        Calendar.YEAR,         // Y
        Calendar.YEAR,         // Y
        Calendar.YEAR,         // Y
        Calendar.YEAR,         // /
        Calendar.DAY_OF_MONTH, // D
        Calendar.DAY_OF_MONTH, // D

        };

    /**
     * Create a DateField with the specified date.
     */
    public JDateFieldAlt(Date date)
    {
        super(20);
        this.addKeyListener(new KeyFocus());
        this.addFocusListener(new FocusClass());
        String myString = dateFormat.format(date);
        setText(myString);
    }

    // Dummy constructor to allo JUnit tests to work
    public JDateFieldAlt()
    {
        this(new Date());
    }

    /**
     * Set the date to the Date mask control.
     */
    public void setDate(Date date)
    {
        setText(dateFormat.format(date));
    }

    /**
     * Get the date from the Date mask control.
     */
    public Date getDate()
    {
        try
        {
            return dateFormat.parse(getText());
        }
        catch (ParseException e)
        {
            return new Date();
        }
        catch (Exception e)
        {
            // DateFormat.parse has some bugs (up to JDK 1.4.2) by which it
            // throws unchecked exceptions. E.g. see:
            // http://developer.java.sun.com/developer/bugParade/bugs/4699765.html
            //
            // To avoid problems with such situations, we'll catch all
            // exceptions here and act just as for ParseException above:
            return new Date();
        }
    }

    /*
     * Convert position in buffer to Calendar type
     * Assumes that pos >=0 (which is true for getCaretPosition())
     */
    private static int posToField(int pos){
        if (pos >= fieldPositions.length) {  // if beyond the end
            pos = fieldPositions.length - 1; // then set to the end
        }
        return fieldPositions[pos];
    }


    /**
     * Converts a date/time to a calendar using the defined format
     */
    private static Calendar parseDate(String datetime)
    {
        Calendar c = Calendar.getInstance();
        try
        {
            Date dat = dateFormat.parse(datetime);
            c.setTime(dat);
        }
        catch (ParseException e)
        {
            //Do nothing; the current time will be returned
        }
        return c;
    }

    /*
     * Update the current field. The addend is only expected to be +1/-1,
     * but other values will work.
     * N.B. the roll() method only supports changes by a single unit - up or down
     */
    private void update(int addend, boolean shifted){
        Calendar c = parseDate(getText());
        int pos = getCaretPosition();
        int field = posToField(pos);
        if (shifted){
            c.roll(field,true);
        } else {
            c.add(field,addend);
        }
        String newDate =dateFormat.format(c.getTime());
        setText(newDate);
        if (pos > newDate.length()) pos = newDate.length();
        setCaretPosition(pos);// Restore position

    }
    /**
     * @author    T.Elanjchezhiyan
     * @version   $Revision: 1.8 $
     */
    class KeyFocus extends KeyAdapter
    {
        KeyFocus()
        {
        }

        public void keyPressed(KeyEvent e)
        {
            if (e.getKeyCode() == KeyEvent.VK_UP)
            {
                update(1,e.isShiftDown());
            }
            else if (e.getKeyCode() == KeyEvent.VK_DOWN)
            {
                update(-1,e.isShiftDown());
            }
        }
    }

    /**
     * @author    T.Elanjchezhiyan
     * @version   $Revision: 1.8 $
     */
    class FocusClass implements FocusListener
    {
        FocusClass()
        {
        }
        public void focusGained(FocusEvent e)
        {
        }
        public void focusLost(FocusEvent e)
        {
            try
            {
                dateFormat.parse(getText());
            }
            catch (ParseException e1)
            {
                requestFocus();
            }
        }
    }
}


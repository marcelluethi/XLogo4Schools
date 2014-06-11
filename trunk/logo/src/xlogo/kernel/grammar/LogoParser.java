/* XLogo4Schools - A Logo Interpreter specialized for use in schools, based on XLogo by Lo�c Le Coq
 * Copyright (C) 2013 Marko Zivkovic
 * 
 * Contact Information: marko88zivkovic at gmail dot com
 * 
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU General Public License as published by the Free 
 * Software Foundation; either version 2 of the License, or (at your option) 
 * any later version.  This program is distributed in the hope that it will be 
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General 
 * Public License for more details.  You should have received a copy of the 
 * GNU General Public License along with this program; if not, write to the Free 
 * Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, 
 * MA 02110-1301, USA.
 * 
 * 
 * This Java source code belongs to XLogo4Schools, written by Marko Zivkovic
 * during his Bachelor thesis at the computer science department of ETH Z�rich,
 * in the year 2013 and/or during future work.
 * 
 * It is a reengineered version of XLogo written by Lo�c Le Coq, published
 * under the GPL License at http://xlogo.tuxfamily.org/
 * 
 * Contents of this file were initially written by Lo�c Le Coq,
 * modifications, extensions, refactorings might have been applied by Marko Zivkovic 
 */

/**
 * Title : XLogo
 * Description : XLogo is an interpreter for the Logo
 * programming language
 * 
 * @author Loïc Le Coq
 */

package xlogo.kernel.grammar;

import xlogo.Logo;
import xlogo.kernel.DrawPanel;
import xlogo.kernel.Primitive;
import xlogo.storage.global.GlobalConfig;

public class LogoParser
{
	private char	c;
	private int		cursor;
	private String	text;
	
	/**
	 * 
	 * @param sr
	 *            The String input reader
	 */
	public LogoParser(String text)
	{
		this.text = text;
		cursor = 0;
		while (cursor < text.length())
		{
			LogoType lt = getToken();
			if (GlobalConfig.DEBUG)
				System.out.println("[DEBUG] Token " + lt.toDebug() + " cursor " + cursor);
		}
	}
	
	private LogoType getToken()
	{
		boolean isQuotedWord = false;
		boolean isVariable = false;
		
		StringBuffer sb = new StringBuffer();
		boolean start = false;
		boolean backslash = false;
		do
		{
			c = text.charAt(cursor);
			// Skip White Spaces
			if (c == ' ' || c == '\t')
			{
				if (start)
					break;
				else
					cursor++;
			}
			else
			{
				if (backslash)
				{
					if (c == ' ')
						sb.append(" ");
					else if (c == '#')
						sb.append("#");
					else if (c == '\\')
						sb.append("\\");
					else if (c == '(')
						sb.append("(");
					else if (c == ')')
						sb.append(")");
					else if (c == '[')
						sb.append("[");
					else if (c == ']')
						sb.append("]");
					else if (c == 'n')
						sb.append("\n");
					else
						sb.append(c);
					cursor++;
				}
				else
				{
					// If it's the first character, check for type
					if (!start)
					{
						if (c == ':')
							isVariable = true;
						else if (c == '\"')
							isQuotedWord = true;
					}
					if (c == '\\')
						backslash = true;
					
					else if (c == '[')
					{
						if (start)
							break;
						else
						{
							cursor++;
							return extractList();
						}
					}
					else if (c == '(')
					{
						if (start)
							break;
					}
					else if (c == '*' || c == '/' || c == '+' || c == '-' || c == '|' || c == '&' || c == '=')
					{
						if (!isQuotedWord)
						{
							if (!start)
							{
								sb.append(c);
								cursor++;
							}
							break;
							
						}
						else
							cursor++;
					}
					else if (c == ')')
						return new LogoException(Logo.messages.getString("parenthese_ouvrante"));
					else if (c == ']')
						return new LogoException(Logo.messages.getString("error.whattodo") + " ]");
					else
					{
						sb.append(c);
						cursor++;
					}
				}
				start = true;
			}
		} while (cursor < text.length());
		if (sb.length() == 0)
			return DrawPanel.nullType;
		else if (isQuotedWord)
			return new LogoWord(sb.substring(1));
		else if (isVariable)
			return new LogoVariable(sb.substring(1));
		try
		{
			double d = Double.parseDouble(sb.toString());
			return new LogoNumber(d);
		}
		catch (NumberFormatException e)
		{
			int id = Primitive.isPrimitive(sb.toString());
			if (id != -1) { return new LogoPrimitive(id, sb.toString()); }
			return new LogoException(Logo.messages.getString("je_ne_sais_pas") + " " + sb.toString());
		}
		
	}
	
	/**
	 * This method extracts a list.
	 * 
	 * @return a LogoList if operation succeed,
	 *         a LogoException otherwise
	 */
	
	private LogoType extractList()
	{
		LogoList list = new LogoList();
		while (cursor < text.length())
		{
			LogoType lt = getListToken();
			if (lt.isNull())
			{
				return new LogoException(Logo.messages.getString("erreur_crochet"));
			}
			else if (lt.isRightDelimiter())
				return list;
			else
			{
				list.add(lt);
			}
		}
		return new LogoException(Logo.messages.getString("erreur_crochet"));
	}
	
	private LogoType getListToken()
	{
		StringBuffer sb = new StringBuffer();
		boolean start = false;
		boolean backslash = false;
		for (int i = cursor; i < text.length(); i++)
		{
			cursor = i;
			c = text.charAt(i);
			// Skip White Spaces
			if (c == ' ' || c == '\t')
			{
				if (start)
					break;
			}
			else
			{
				if (backslash)
				{
					if (c == ' ')
						sb.append(" ");
					else if (c == '#')
						sb.append("#");
					else if (c == '\\')
						sb.append("\\");
					else if (c == '(')
						sb.append("(");
					else if (c == ')')
						sb.append(")");
					else if (c == '[')
						sb.append("[");
					else if (c == ']')
						sb.append("]");
					else if (c == 'n')
						sb.append("\n");
					else
						sb.append(c);
				}
				else
				{
					if (c == '\\')
					{
						backslash = true;
					}
					else if (c == '[')
					{
						if (start)
							break;
						else
						{
							cursor++;
							return extractList();
						}
					}
					else if (c == ']')
					{
						if (start)
							break;
						else
						{
							System.out.println("coucou");
							cursor++;
							return new LogoRightDelimiter();
						}
					}
					else
						sb.append(c);
				}
				start = true;
			}
		}
		if (sb.length() == 0)
			return DrawPanel.nullType;
		return new LogoWord(sb.toString());
	}
	
}

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

/** Title : XLogo
 * Description : XLogo is an interpreter for the Logo
 * programming language
 * 
 * @author Loïc Le Coq */
package xlogo.kernel;

import java.util.Stack;
import java.util.HashMap;

import xlogo.Application;
import xlogo.Logo;
import xlogo.kernel.userspace.UserSpace;
import xlogo.kernel.userspace.procedures.Procedure;

public class Interprete
{
	
	private LaunchPrimitive							lanceprim;
	
	private Application								app;
	
	private Kernel									kernel;
	
	private UserSpace								wp;
	protected static boolean						renvoi_instruction	= false;
	public static Stack<String>						calcul				= new Stack<String>();
	protected static Stack<HashMap<String, String>>	stockvariable		= new Stack<HashMap<String, String>>();
	protected static boolean						stop				= false;
	protected static Stack<String>					nom					= new Stack<String>();
	// procédures attendant
	// parmamètres à recevoir
	public static Stack<String>						en_cours			= new Stack<String>();
	// actuellement en cours
	// d'exécution
	/**
	 * This buffer contains all instructions to execute
	 * 
	 * @uml.property name="instructionBuffer"
	 * @uml.associationEnd multiplicity="(1 1)"
	 */
	private InstructionBuffer						instructionBuffer	= new InstructionBuffer();
	
	public static StringBuffer						actionInstruction	= new StringBuffer();
	
	protected static HashMap<String, String>		locale				= new HashMap<String, String>();
	// noms des variables
	// locales
	
	protected static boolean						operande			= false;
	protected static boolean						operateur			= false;
	protected static boolean						drapeau_ouvrante	= false;
	protected static boolean						drapeau_fermante	= false;
	public static String							lineNumber			= "";
	
	// private TreeParser tp;
	/*
	 * public Interprete(Application cadre){ this.cadre=cadre;
	 * lanceprim=new LaunchPrimitive(cadre); cadre.error=false; }
	 */
	public Interprete(Application app)
	{
		this.kernel = app.getKernel();
		this.app = app;
		wp = kernel.getWorkspace();
		lanceprim = new LaunchPrimitive(app, wp);
		app.error = false;
	}
	
	String execute(StringBuffer instructions) throws LogoError
	{
		if (!instructions.equals(""))
		{
			instructionBuffer.insertCode(instructions);
		}
		
		// Object obca1,obca2,oban;
		while (instructionBuffer.getLength() != 0)
		{
			if (app.error) // TODO  && LogoError.lance : before, lance was always false, thus this branch was never taken. check this.
				throw new LogoError(Logo.messages.getString("stop"));
			while (app.affichage.isOnPause())
			{ // Si l'on touche aux scrollbars
				try
				{
					wait();
				}
				catch (Exception e)
				{}
			}
			// System.out.println("en_cours d'execution "+"\n"+
			// en_cours+"\n\n");
			// System.out.println("nom "+nom);
			// System.out.println("calcul \n"+calcul+"\n\n");
			// System.out.println("nom "+nom.toString()+" locale "+locale+ "
			// "+valeur+" stockvariable "+stockvariable);
			// System.out.println("operande "+calcul+" "+operande+"debut"+instructionBuffer);
			
			// Is this line really interesting??
			if (instructionBuffer.getLength() == 0)
				break;
			// System.out.print("debut\n"+instructionBuffer+"\nfin\n------------------\n");
			String element = instructionBuffer.getNextWord();
			// System.out.println(app.affichage.getPause()+element);
			
			// System.out.println("/"+instructionBuffer+"/");
			
			/*
			 * if (element=="")
			 * break;
			 *//* ***********************************************
				 * // si c'est une primitive ou une procedure *******
				 * ***********************************************
				 */
			String element_minuscule = element.toLowerCase();
			/*
			 * Marko Zivkovic: In XLogo4Schools, a procedure is identified by its name, not by its position in a stack.
			 * Now, i denotes only Logo primitives. If element_minuscule is a procedure, then i will be set to -2,
			 * s.t. the interpreter can assume as before that it denotes something other than primitive.
			 * If i >= 0, then the interpreter will work just as before, using i.
			 */
			boolean isProcedure = wp.isExecutable(element_minuscule);
			int i;
			
			if (Primitive.primitives.containsKey(element_minuscule) || isProcedure)
			{
				
				// identifiant de la primitive
				if (!isProcedure)
				{
					i = Integer.valueOf(Primitive.primitives.get(element_minuscule)).intValue()
							% Primitive.PRIMITIVE_NUMBER;
				}
				else
				{
					//i = -i - 2;
					i = -2; // Marko Zivkovic : i now denotes something else than a primitive : if i < -1 => isProcedure
				}
				// if (!calcul.empty()&&nom.isEmpty())
				// throw new
				// monException(cadre,Logo.messages.getString("que_faire")+"
				// "+calcul.pop() +" gdfdsf");
				// exécuter la procédure ou la primitive.
				Stack<String> param = new Stack<String>();
				if (isInfixedOperator(i))
				{ // Si c'est un opérateur infixé
					deleteLineNumber();
					operateur = true;
					operande = false;
					/*
					 * if (drapeau_ouvrante) { drapeau_ouvrante=false;
					 * if (i!=32&&i!=33) throw new myException(element+"
					 * "+Logo.messages.getString("ne_peut_etre")); else
					 * param.push("0"); }
					 */
					// else
					if (calcul.isEmpty())
					{ // Si le + ou le - représente le
						// signe négatif ou positif
						if (i != 32 && i != 33)
							throw new LogoError(element + " " + Logo.messages.getString("error.ne_peut_etre")); // d'un
						// nombre
						if (nom.isEmpty())
							param.push("0");
						else
						{
							String st = nom.peek();
							if (!testoperateur(st))
								param.push("0");
							else if ("*/".indexOf(st) > -1)
							{ // Si le signe -
								// ou + suit un
								// * ou /
								instructionBuffer.deleteFirstWord(element);
								if (st.equals("*"))
									instructionBuffer.insert("* ");
								else
									instructionBuffer.insert("/ ");
								if (i == 32)
									return ("1"); // Si c'est un plus
								else
									return ("-1"); // Si c'est un moins
							}
							else
								param.push("0");
						}
					}
					else if (nom.isEmpty())
					{
						param.push(calcul.pop());
					}
					else
					{
						String st = nom.peek();
						if (testoperateur(st))
						{
							// System.out.println("st "+st+" element "+element+"
							// "+prioriteinf(st,element));
							if (prioriteinf(st, element))
							{
								param.push(calcul.pop());
							}
							else
								return (calcul.pop());
						}
						else
							param.push(calcul.pop());
					}
				}// END: INFIX OPERATOR
				else if (operande && i != 204)
				{
					checkParenthesis();
					operande = false;
					break;
				} // Si ce n'est pas l'opérateur de fin de parenthèse, on sort
				
				/*
				 * Example:
				 * To test | Formatted Form:
				 * fd 5 | fd 5 \l1 rt \l2
				 * rt | --> The \l2 can't be removed before be
				 * end | sure the rt has noproblem
				 */
				if (!element.equals("\n"))
					deleteLineNumber();
				instructionBuffer.deleteFirstWord(element);
				
				// Case with parenthensis
				// eg (sum 3 4 5)
				// eg (myProcedure 2 3 4 5)
				if (drapeau_ouvrante)
				{
					drapeau_ouvrante = false;
					int constantNumber = -1;
					if (!hasGeneralForm(element_minuscule, i))
					{
						if (!isProcedure)
							constantNumber = kernel.primitive.parametres[i];
						else
							constantNumber = wp.getExecutable(element_minuscule).nbparametre;
					}
					// Looking for all arguments (Number undefined)
					nom.push(element);
					int j = 0;
					while (true)
					{
						/*
						 * This line fixed the bug for primitive or procedure
						 * without arguments
						 * eg: pr (pi+2)
						 */
						if (constantNumber == 0)
							break;
						try
						{
							operande = operateur = drapeau_ouvrante = false;
							if (instructionBuffer.getNextWord().equals(")"))
							{
								if (constantNumber != -1)
								{
									// If the primitive or the procedure doesn't
									// accept optional parameters
									if (j > constantNumber)
									{
										throw new LogoError(Logo.messages.getString("too_much_arguments"));
									}
									else if (j < constantNumber)
										throw new LogoError(Logo.messages.getString("pas_assez_de") + " " + nom.peek());
								}
								break;
							}
							String a = execute(new StringBuffer());
							
							param.push(a);
						}
						catch (LogoError e)
						{
							throw e;
						}
						j++;
					}
					// If It's a procedure
					
					if (isProcedure)
					{ 
						if (wp.isProcedureAmbiguous(element_minuscule))
							throw new LogoError(Logo.messages.getString("error.call.ambiguous.procedure.name"));
						
						Procedure proc = wp.getExecutable(element_minuscule);
						
						if (j > proc.nbparametre + proc.optVariables.size())
							throw new LogoError(Logo.messages.getString("too_much_arguments"));
						else if (j < proc.nbparametre)
							throw new LogoError(Logo.messages.getString("pas_assez_de") + " " + nom.peek());
						// Searching for optional arguments that are not defined
						
						if (j < proc.optVariables.size() + proc.nbparametre)
						{
							j = j - proc.nbparametre;
							for (int c = j; c < proc.optVariables.size(); c++)
							{
								try
								{
									operande = operateur = drapeau_ouvrante = false;
									String a = execute(proc.optVariablesExp.get(c));
									param.push(a);
								}
								catch (LogoError e)
								{
									throw e;
								}
							}
						}
					}
					
				}
				// classic case: predefined number of arguments
				else
				{
					drapeau_ouvrante = false;
					// How many arguments for the procedure or the primitive
					int nbparametre = 0;
					// For primitive
					if (!isProcedure)
						nbparametre = kernel.primitive.parametres[i];
					// For procedure
					else
						nbparametre = wp.getExecutable(element_minuscule).nbparametre;
					// Looking for each arguments
					int j = 0;
					nom.push(element);
					while (j < nbparametre)
					{
						try
						{
							operande = operateur = drapeau_ouvrante = false;
							
							String a = execute(new StringBuffer());
							param.push(a);
							j++;
						}
						catch (LogoError e)
						{
							throw e;
						}
					}
					// System.out.println(instructionBuffer.toString());
					// System.out.println(nom+"arguments"+param);
					// Looking for Optional arguments in case of procedure
					if (isProcedure)
					{
						Procedure proc = wp.getExecutable(element_minuscule);
						nbparametre = proc.optVariables.size();
						for (j = 0; j < nbparametre; j++)
						{
							try
							{
								operande = operateur = drapeau_ouvrante = false;
								String a = execute(proc.optVariablesExp.get(j));
								param.push(a);
							}
							catch (LogoError e)
							{
								throw e;
							}
						}
					}
				}
				
				// //////////////////////////////////////////////////////////////////////////////////////////
				// System.out.println(nom+" "+"debut "+instruction+"
				// fin\n"+param.toString());
				// System.out.println(nom);
				nom.pop();
				if (!app.error)
					lanceprim.execute(i, element_minuscule, param);
				if (app.error)
					break;
				if (drapeau_fermante && !calcul.empty())
				{
					drapeau_fermante = false;
					operande = false;
					return calcul.pop();
				}
				
				// Tester si la procédure rend quelque chose lorsqu'attendu
				
				if (!operande)
				{
					// dans le cas des primitives exec ou si
					if (renvoi_instruction)
					{
						renvoi_instruction = false;
					}
					else
					{
						if (!nom.isEmpty() && !app.error && !nom.peek().equals("\n"))
						{
							if (!element.equals("\n"))
							{
								// If it's the end of a loop
								// repeat 2 [fd 90 rt]
								if (element.equals("\\"))
								{
									// The loop had been executed, we have to
									// remove
									// the loop instruction
									int offset = instructionBuffer.indexOf(" \\ ");
									instructionBuffer.delete(0, offset + 1);
									
									throw new LogoError(Logo.messages.getString("pas_assez_de") + " " + nom.peek());
								}
								// (av 100) ---> OK
								// av av 20 ----> Bad
								if (!nom.peek().equals("("))
									throw new LogoError(element + " " + Logo.messages.getString("ne_renvoie_pas") + " "
											+ nom.peek());
								
							}
						}
					}
				}
				else
				{
					// The primitive returns a word or a list.
					// There's no primitive or procedure waiting for it.
					if (!nom.isEmpty() && nom.peek().equals("\n"))
						throw new LogoError(Logo.messages.getString("error.whattodo") + " " + calcul.peek() + " ?");
				}
			}
			
			/* ********************************
			 * / IF element IS A VARIABLE
			 * ********************************
			 */
			else if (element.substring(0, 1).equals(":") && element.length() > 1)
			{
				// System.out.println(operande);
				if (operande)
				{
					checkParenthesis();
					operande = false;
					break;
				}
				else
					deleteLineNumber();
				String value;
				String variableName = element_minuscule.substring(1, element_minuscule.length());
				// If the variable isn't local
				if (!locale.containsKey(variableName))
				{
					// check it's a global variable
					if (!wp.getGlobals().getVariables().contains(variableName)) // TODO
																				// CHECK
						throw new LogoError(variableName + " " + Logo.messages.getString("error.novalue"));
					else
						value = wp.getGlobals().getValue(variableName).toString(); // TODO
																					// CHECK
				}
				// If the variable is local
				else
				{
					value = locale.get(variableName);
				}
				
				if (null == value)
					throw new LogoError(variableName + "  " + Logo.messages.getString("error.novalue"));
				calcul.push(value);
				operande = true;
				operateur = false;
				drapeau_ouvrante = false;
				instructionBuffer.deleteFirstWord(element);
			}
			else
			{
				/* *****************************
				 * IF element IS A NUMBER ******
				 * **************************
				 */
				try
				{
					Double.parseDouble(element);
					boolean deleteEndZero = false;
					if (element.endsWith(".0"))
					{
						deleteEndZero = true;
						element = element.substring(0, element.length() - 2);
					}
					/*
					 * boolean addStartZero=false;
					 * if (element.startsWith(".") || element.equals("")){
					 * element = "0" + element;
					 * addStartZero=true;
					 * }
					 */
					calcul.push(element);
					if (operande)
					{
						checkParenthesis();
						calcul.pop();
						operande = false;
						break;
					}
					else
						deleteLineNumber();
					operande = true;
					operateur = false;
					drapeau_ouvrante = false;
					// if (addStartZero)
					// instructionBuffer.deleteFirstWord(element.substring(1));
					if (deleteEndZero)
						instructionBuffer.deleteFirstWord(element + ".0");
					else
						instructionBuffer.deleteFirstWord(element);
					
				}
				catch (NumberFormatException e)
				{
					/* *********************************
					 * IF element IS A SQUARE BRACKET [
					 * OPEN
					 * **********************************
					 */
					if (element.equals("["))
					{
						
						// Utilité de cette ligne?
						// if (!calcul.isEmpty()&&operateur==false) break;
						if (operande)
						{
							checkParenthesis();
							break;
						}
						else
							deleteLineNumber();
						operande = true;
						operateur = false;
						drapeau_ouvrante = false;
						instructionBuffer.deleteFirstWord(element);
						String a = chercheListe();
						calcul.push(a);
					}
					/* ***************************
					 * IF element IS A PARENTHESIS
					 * OPEN
					 * ***********************
					 */
					else if (element.equals("("))
					{
						if (operande)
						{
							checkParenthesis();
							break;
						}
						else
							deleteLineNumber();
						drapeau_ouvrante = true;
						
						Interprete.en_cours.push("(");
						int pos = chercheParenthese();
						if (pos == -1)
						{
							try
							{
								throw new LogoError(Logo.messages.getString("parenthese_fermante"));
							}
							catch (LogoError e1)
							{}
						}
						instructionBuffer.deleteFirstWord(element);
						// System.out.println("&&"+instruction);
						Interprete.nom.push("(");
					}
					/* **********************************
					 * IF element IS A WORD
					 * **************************
					 */
					else if (element.substring(0, 1).equals("\""))
					{
						try
						{
							String el = element.substring(1);
							Double.parseDouble(el);
							calcul.push(el);
						}
						catch (NumberFormatException e1)
						{
							calcul.push(element);
						}
						if (operande)
						{
							checkParenthesis();
							calcul.pop();
							operande = false;
							break;
						}
						else
							deleteLineNumber();
						operande = true;
						operateur = false;
						drapeau_ouvrante = false;
						instructionBuffer.deleteFirstWord(element);
					}
					// Si c'est le mot pour
					else if (element_minuscule.equals(Logo.messages.getString("pour")))
					{
						instructionBuffer.deleteFirstWord(element);
						if (instructionBuffer.getLength() != 0)
						{
							element = instructionBuffer.getNextWord();
							element_minuscule = element.toLowerCase();
						}
						else
							throw new LogoError(Logo.messages.getString("pas_assez_de") + " " + "\""
									+ Logo.messages.getString("pour") + "\"");
						if (Primitive.primitives.containsKey(element_minuscule) || wp.isExecutable(element_minuscule)) // TODO
																														// check
							throw new LogoError(element + " " + Logo.messages.getString("existe_deja"));
						else
						{
							String definition = Logo.messages.getString("pour") + " " + element + " ";
							instructionBuffer.deleteFirstWord(element);
							while (instructionBuffer.getLength() != 0)
							{
								element = instructionBuffer.getNextWord().toLowerCase();
								if (null == element)
									break;
								if (!element.substring(0, 1).equals(":") || element.length() == 1)
									throw new LogoError(element + " " + Logo.messages.getString("pas_argument"));
								definition += element + " ";
								instructionBuffer.deleteFirstWord(element);
							}
							if (app.editeur.getComponent().isVisible())
								throw new LogoError(Logo.messages.getString("ferme_editeur"));
							else
							{
								app.editeur.getComponent().setVisible(true);
								app.editeur.setEditorStyledText(definition + "\n\n" + Logo.messages.getString("fin"));
							}
						}
					}
					else if (element.startsWith("\\l"))
					{
						if (operande)
						{
							break;
						}
						instructionBuffer.deleteFirstWord(element);
						lineNumber = element + " ";
						element = instructionBuffer.getNextWord();
						
					}
					else
					{
						deleteLineNumber();
						throw new LogoError(Logo.messages.getString("je_ne_sais_pas") + " " + element);
					}
				}
			}
			// System.out.println("instruction "+instruction+" calcul "+calcul);
		}
		/* ******************************
		 * **** END OF THE MAIN LOOP
		 * *****************************
		 */
		// S'il n'y a rien à retourner.
		if (calcul.isEmpty())
		{
			if (!nom.isEmpty())
			{// &&!nom.peek().equals("\n")) {
				while ((!nom.isEmpty()) && nom.peek().equals("\n"))
					nom.pop();
				if (!nom.isEmpty()) { throw new LogoError(Logo.messages.getString("pas_assez_de") + " " + nom.peek()); }
			}
		}
		// Sinon on retourne la valeur contenue dans la pile de calcul.
		if (!calcul.isEmpty())
		{
			// S'il y a une procédure de lancer
			// Ex: pour t -- 6 -- fin . Puis, av t.
			if ((!nom.isEmpty()) && nom.peek().equals("\n"))
			{
				String up = "";
				int id = 0;
				while (!nom.isEmpty() && nom.peek().equals("\n"))
				{
					nom.pop();
					id++;
				}
				if (!nom.isEmpty())
				{
					up = nom.peek().toString();
					try
					{
						throw new LogoError(en_cours.get(en_cours.size() - id) + " "
								+ Logo.messages.getString("ne_renvoie_pas") + " " + up);
					}
					catch (LogoError e)
					{}
				}
				else
				{
					try
					{
						throw new LogoError(Logo.messages.getString("error.whattodo") + " " + calcul.peek() + " ?");
					}
					catch (LogoError e)
					{}
				}
				/*
				 * }
				 * if (!nom.isEmpty() && nom.peek().equals("\n")) {
				 * up = en_cours.get(en_cours.size() - 2).toString();
				 * try {
				 * throw new myException( en_cours.peek() + " "
				 * + Logo.messages.getString("ne_renvoie_pas")
				 * + " " + up);
				 * } catch (myException e) {
				 * }
				 * } else if (!nom.isEmpty()) {
				 * up = nom.peek().toString();
				 * try {
				 * throw new myException( en_cours.peek() + " "
				 * + Logo.messages.getString("ne_renvoie_pas")
				 * + " " + up);
				 * } catch (myException e) {
				 * }
				 * } else {
				 * try {
				 * throw new myException( Logo.messages
				 * .getString("que_faire")
				 * + " " + calcul.peek() + " ?");
				 * } catch (myException e) {
				 * }}
				 */
				
			}
			// ///////////
			else
			{
				operande = false;
				return (calcul.pop());
			}
		}
		return ("");
	}
	
	private int chercheParenthese()
	{ // position ou s'arrete la prochaine
		// parenthese
		boolean continuer = true;
		int of_ouvrant;
		int of_fermant = 0;
		int from_index_ouvrant = 1;
		int from_index_fermant = 1;
		while (continuer)
		{
			of_ouvrant = instructionBuffer.indexOf("(", from_index_ouvrant);
			of_fermant = instructionBuffer.indexOf(")", from_index_fermant);
			if (of_fermant == -1)
				break;
			if (of_ouvrant != -1 && of_ouvrant < of_fermant)
			{
				from_index_ouvrant = of_ouvrant + 1;
				from_index_fermant = of_fermant + 1;
			}
			else
				continuer = false;
			;
		}
		return of_fermant;
	}
	
	protected String chercheListe() throws LogoError
	{
		String liste = "[ ";
		String element = "";
		while (instructionBuffer.getLength() != 0)
		{
			element = instructionBuffer.getNextWord();
			// SI crochet ouvrant, on l'empile dans la pile de calcul
			if (element.equals("["))
			{
				calcul.push("[");
				instructionBuffer.deleteFirstWord(element);
				liste += "[ ";
			}
			
			else if (element.equals("]"))
			{ // Si on atteint un crochet fermant
				instructionBuffer.deleteFirstWord(element);
				// if (((Stack)instruction.peek()).isEmpty()) instruction.pop();
				liste += "] ";
				if (calcul.empty())
				{
					return (liste);
				} // 1er cas: rien dans la pile de calcul, on renvoie la liste
				else if (!calcul.peek().toString().equals("["))
				{
					return (liste);
				} // 2eme cas: pas de crochet ouvrant en haut de la pile, idem
				else
					calcul.pop(); // 3eme cas: un crochet ouvrant en haut de
									// la pile, on l'enleve
			}
			else
			{
				instructionBuffer.deleteFirstWord(element);
				liste += element + " ";
			}
		}
		throw new LogoError(Logo.messages.getString("erreur_crochet"));
	}
	
	private boolean testoperateur(String st)
	{ // l'élément trouvé est-il un
		// opérateur
		int i = "+-*/<>=!&|".indexOf(st);
		if (i == -1)
			return (false);
		return (true);
	}
	
	/**
	 * This method compares the two operators op and str.<br>
	 * Cette methode teste si l'operateur op (sommet de la pile d'appel)
	 * est de priorite strictement inferieur a str
	 * 
	 * @param op
	 *            The first operator
	 * @param str
	 *            The second operator
	 * @return true if op has a minor priority than str.
	 */
	
	private boolean prioriteinf(String op, String str)
	{ /*
		* if (parenthesefermante>0) return(false); //
		* else
		*/
		if (isTimesDiv(str) && !isTimesDiv(op))
			return (true);
		else if (isPlusMinus(str) && isLogicOperator(op))
			return (true);
		else if (">=<=".indexOf(str) > -1 && "|&".indexOf(op) > -1)
			return (true);
		return (false);
	}
	
	/*
	 * private int isProcedure(String mot) { // vérifie si mot est une
	 * procédure
	 * for (int i = 0; i < wp.getNumberOfProcedure(); i++) {
	 * if (wp.getProcedure(i).name.equals(mot))
	 * return (i);
	 * }
	 * return (-1);
	 * }
	 */
	
	protected void setWorkspace(UserSpace workspace)
	{
		wp = workspace;
		lanceprim.setWorkspace(workspace);
	}
	
	private boolean hasGeneralForm(String name, int i)
	{
		// If it's a procedure
		if (wp.isExecutable(name))
			return !wp.getExecutable(name).optVariables.isEmpty();
		return kernel.primitive.generalForm[i];
	}
	
	private void checkParenthesis() throws LogoError
	{
		if (!nom.isEmpty())
		{
			String name = nom.peek();
			if (name.equals("(")) { throw new LogoError(Logo.messages.getString("too_much_arguments"));
			
			}
		}
		
	}
	
	private void deleteLineNumber()
	{
		lineNumber = "";
	}
	
	/**
	 * This method indicates if a primitive is an infixed operator<br>
	 * Infixed operators are for example: +,-,*-,/,&,>=.....
	 * 
	 * @param id
	 *            The integer identifiant for the primitive
	 * @return true or false if it's an infixed operator
	 */
	private boolean isInfixedOperator(int id)
	{
		boolean b1 = (29 < id) && (id < 39);
		boolean b2 = (id == 273) || (id == 274);
		return b1 || b2;
	}
	
	/**
	 * This metods tests if the String op is a logic operator, ie a string like
	 * |,&,<,>,=,>=,<=
	 * 
	 * @param op
	 *            The operator to test
	 * @return true if op is a logic operator
	 */
	private boolean isLogicOperator(String op)
	{
		return ("|&>=<=".indexOf(op) != -1);
		
	}
	
	/**
	 * This metods tests if the String op is + or -
	 * 
	 * @param op
	 *            The operator to test
	 * @return true if op is + or -
	 */
	private boolean isPlusMinus(String op)
	{
		return (op.equals("+") || op.equals("-"));
	}
	
	/**
	 * This metods tests if the String op is / or *
	 * 
	 * @param op
	 *            The operator to test
	 * @return true if op is * or /
	 */
	private boolean isTimesDiv(String op)
	{
		return (op.equals("*") || op.equals("/"));
	}
	
	/**
	 * @return
	 * @uml.property name="instructionBuffer"
	 */
	protected InstructionBuffer getInstructionBuffer()
	{
		return instructionBuffer;
	}
}

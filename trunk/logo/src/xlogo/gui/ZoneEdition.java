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

package xlogo.gui;

import javax.swing.event.*;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.SwingUtilities;

import xlogo.StyledDocument.DocumentLogo;
import xlogo.storage.WSManager;

/**
 * Title :        XLogo
 * Description :  XLogo is an interpreter for the Logo 
 * 						programming language
 * @author Loïc Le Coq
 */
class ZoneEdition extends JTextPane implements CaretListener{
	private static final long serialVersionUID = 1L;
	private DocumentLogo dsd=null;
	// Si la correspondance entre parenthese ou crochets est activée
	private boolean active=false;
	// Dernière position allumée
	private int[] position=new int[2];
	

//	public DocumentLogo getDsd(){
//		return dsd;
//	}
	public void setActive(boolean b){
		active=b;
	}
	ZoneEdition(EditorTextPane etp){
	dsd=etp.getDsd();
	addCaretListener(this);
}

// Teste si le caractère précédent est un backslash
private boolean TesteBackslash(String content,int pos){
	String caractere="";
	if (pos>0) caractere=content.substring(pos-1,pos);
	//System.out.println(caractere);
	if (caractere.equals("\\")) return true;
	return false;
}

class verif_parenthese implements Runnable{
	int pos;
	verif_parenthese(int pos){
	this.pos=pos;	
	}
	public void run(){
		if (active){
			active=false;
			int debut=position[0];
			int fin=position[0];
			if (debut!=-1){
				if (debut>0) debut--;
				if (fin<dsd.getLength()) fin++;
				try{			
					String content=dsd.getText(0,dsd.getLength());
					dsd.colore(content,debut,fin);	
				}
				catch(BadLocationException e){}
			}
			debut=position[1];
			fin=position[1];
			if (debut!=-1){
				if (debut>0) debut--;
				if (fin<dsd.getLength()) debut++;
				if (fin<dsd.getLength()) fin++;
				try{			
					String content=dsd.getText(0,dsd.getLength());
					dsd.colore(content,debut,fin);	
				}
				catch(BadLocationException e){}
			}
		}
		int length=dsd.getLength();
		try{
			String content=dsd.getText(pos,length-pos);
			int id=-1;
			if (length>pos)	{
				id="[]()".indexOf(content.substring(0,1));
			}
			if (id>-1&&!TesteBackslash(dsd.getText(0,pos),pos)){
				active=true;
				switch(id){
				case 0: 
					chercheApres(content,pos,"[","]");
					break;
				case 1:
					content=getText(0,pos);
					chercheAvant(content,pos,"[","]");
				break;
				case 2:
					chercheApres(content,pos,"(",")");
				break;
				case 3:
					content=getText(0,pos);
					chercheAvant(content,pos,"(",")");
				break;
				}
			}
		}
		catch(BadLocationException e1){}

	}
}
public void caretUpdate(CaretEvent e){
	int pos=e.getDot();
	if (WSManager.getWorkspaceConfig().isSyntaxHighlightingEnabled()) SwingUtilities.invokeLater(new verif_parenthese(pos));
}
	void chercheApres(String content,int pos,String ouv,String fer){
		boolean continuer=true;
		int of_ouvrant;
		int of_fermant=0;
		int from_index_ouvrant=1;
		int from_index_fermant=1;
		while(continuer){
			of_ouvrant=content.indexOf(ouv,from_index_ouvrant);
			while (of_ouvrant!=-1&&TesteBackslash(content,of_ouvrant)) of_ouvrant=content.indexOf(ouv,of_ouvrant+1);
			of_fermant=content.indexOf(fer,from_index_fermant);
			while (of_fermant!=-1&&TesteBackslash(content,of_fermant)) of_fermant=content.indexOf(fer,of_fermant+1);
			if (of_fermant==-1) break;
			if (of_ouvrant!=-1 && of_ouvrant<of_fermant) {
				from_index_ouvrant=of_ouvrant+1;
				from_index_fermant=of_fermant+1;
			}
			else continuer=false;;
		}
		if (of_fermant!=-1) {
			dsd.Montre_Parenthese(of_fermant+pos);
			position[1]=of_fermant+pos;
		}
		else position[1]=-1;
		dsd.Montre_Parenthese(pos);
		position[0]=pos;
	}
	void chercheAvant(String content,int pos,String ouv,String fer){
		boolean continuer=true;
		int of_fermant=0;
		int of_ouvrant=0;
		int from_index_ouvrant=pos;
		int from_index_fermant=pos;
		while(continuer){
			of_ouvrant=content.lastIndexOf(ouv,from_index_ouvrant);
			while (of_ouvrant!=-1&&TesteBackslash(content,of_ouvrant)) of_ouvrant=content.lastIndexOf(ouv,of_ouvrant-1);			
			of_fermant=content.lastIndexOf(fer,from_index_fermant);
			while (of_fermant!=-1&&TesteBackslash(content,of_fermant)) of_fermant=content.lastIndexOf(fer,of_fermant-1);
			if (of_ouvrant==-1) break;
			if (of_ouvrant<of_fermant) {
				from_index_ouvrant=of_ouvrant-1;
				from_index_fermant=of_fermant-1;
			}
			else continuer=false;;
		}
		if (of_ouvrant!=-1) {
			dsd.Montre_Parenthese(of_ouvrant);
			position[0]=of_ouvrant;
		}
		else position[0]=-1;
		dsd.Montre_Parenthese(pos);
		position[1]=pos;
	
	
	}

}
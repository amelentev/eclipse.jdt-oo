/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.parser;

import java.util.List;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.Javadoc;
import org.eclipse.jdt.internal.compiler.ast.JavadocAllocationExpression;
import org.eclipse.jdt.internal.compiler.ast.JavadocArgumentExpression;
import org.eclipse.jdt.internal.compiler.ast.JavadocArrayQualifiedTypeReference;
import org.eclipse.jdt.internal.compiler.ast.JavadocArraySingleTypeReference;
import org.eclipse.jdt.internal.compiler.ast.JavadocFieldReference;
import org.eclipse.jdt.internal.compiler.ast.JavadocMessageSend;
import org.eclipse.jdt.internal.compiler.ast.JavadocQualifiedTypeReference;
import org.eclipse.jdt.internal.compiler.ast.JavadocReturnStatement;
import org.eclipse.jdt.internal.compiler.ast.JavadocSingleNameReference;
import org.eclipse.jdt.internal.compiler.ast.JavadocSingleTypeReference;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.problem.ProblemSeverities;

/**
 * Parser specialized for decoding javadoc comments
 */
public class JavadocParser extends AbstractCommentParser {

	// Public fields
	public Javadoc javadoc;
	public boolean checkJavadoc = false;
	
	// Private fields
	private int linePtr, lastLinePtr;

	JavadocParser(Parser sourceParser) {
		super(sourceParser);
		this.checkJavadoc = (this.sourceParser.options.getSeverity(CompilerOptions.InvalidJavadoc) != ProblemSeverities.Ignore) ||
			(this.sourceParser.options.getSeverity(CompilerOptions.MissingJavadocTags) != ProblemSeverities.Ignore);
	}

	/* (non-Javadoc)
	 * Returns true if tag @deprecated is present in javadoc comment.
	 * 
	 * If javadoc checking is enabled, will also construct an Javadoc node, which will be stored into Parser.javadoc
	 * slot for being consumed later on.
	 */
	public boolean checkDeprecation(int javadocStart, int javadocEnd) {

		try {
			this.source = this.sourceParser.scanner.source;
			this.index = javadocStart +3;
			this.endComment = javadocEnd - 2;
			if (this.checkJavadoc) {
				// Initialization
				this.javadoc = new Javadoc(javadocStart, javadocEnd);
				super.checkDeprecation(javadocStart, javadocEnd);
			} else {
				// Init javadoc if necessary
				if (this.sourceParser.options.getSeverity(CompilerOptions.MissingJavadocComments) != ProblemSeverities.Ignore) {
					this.javadoc = new Javadoc(javadocStart, javadocEnd);
				} else {
					this.javadoc = null;
				}
				
				// Parse comment
				int firstLineNumber = this.sourceParser.scanner.getLineNumber(javadocStart);
				int lastLineNumber = this.sourceParser.scanner.getLineNumber(javadocEnd);
	
				// scan line per line, since tags must be at beginning of lines only
				nextLine : for (int line = firstLineNumber; line <= lastLineNumber; line++) {
					int lineStart = line == firstLineNumber
							? javadocStart + 3 // skip leading /**
							: this.sourceParser.scanner.getLineStart(line);
					this.index = lineStart;
					this.lineEnd = line == lastLineNumber
							? javadocEnd - 2 // remove trailing * /
							: this.sourceParser.scanner.getLineEnd(line);
					while (this.index < this.lineEnd) {
						char nextCharacter = readChar(); // consider unicodes
						if  (nextCharacter == '@' &&
							(readChar() == 'd') &&
							(readChar() == 'e') &&
							(readChar() == 'p') &&
							(readChar() == 'r') &&
							(readChar() == 'e') &&
							(readChar() == 'c') &&
							(readChar() == 'a') &&
							(readChar() == 't') &&
							(readChar() == 'e') &&
							(readChar() == 'd'))
						{
							// ensure the tag is properly ended: either followed by a space, a tab, line end or asterisk.
							nextCharacter = readChar();
							if (Character.isWhitespace(nextCharacter) || nextCharacter == '*') {
								return true;
							}
						}
					}
				}
				return false;
			}
		} finally {
			this.source = null; // release source as soon as finished
		}
		return this.deprecated;
	}

	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("check javadoc: ").append(this.checkJavadoc).append("\n");	//$NON-NLS-1$ //$NON-NLS-2$
		buffer.append("javadoc: ").append(this.javadoc).append("\n");	//$NON-NLS-1$ //$NON-NLS-2$
		buffer.append(super.toString());
		return buffer.toString();
	}

	protected void initLineEnd() {
		this.linePtr = this.sourceParser.scanner.getLineNumber(this.javadoc.sourceStart);
		this.lastLinePtr = this.sourceParser.scanner.getLineNumber(this.javadoc.sourceEnd);
		this.lineEnd = (this.linePtr == this.lastLinePtr) ? this.endComment : this.javadoc.sourceStart + 3;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.compiler.parser.AbstractCommentParser#createArgumentReference(char[], java.lang.Object, int)
	 */
	protected Object createArgumentReference(char[] name, int dim, Object typeRef, int argEnd) throws InvalidInputException {
		try {
			TypeReference argTypeRef = (TypeReference) typeRef;
			if (dim > 0) {
				long pos = ((long) argTypeRef.sourceStart) << 32 + argTypeRef.sourceEnd;
				if (typeRef instanceof JavadocSingleTypeReference) {
					JavadocSingleTypeReference singleRef = (JavadocSingleTypeReference) typeRef;
					argTypeRef = new JavadocArraySingleTypeReference(singleRef.token, dim, pos);
				} else {
					JavadocQualifiedTypeReference qualifRef = (JavadocQualifiedTypeReference) typeRef;
					argTypeRef = new JavadocArrayQualifiedTypeReference(qualifRef, dim);
				}
			}
			return new JavadocArgumentExpression(name, argTypeRef.sourceStart, argEnd, argTypeRef);
		}
		catch (ClassCastException ex) {
				throw new InvalidInputException();
		}
	}
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.compiler.parser.AbstractCommentParser#createFieldReference()
	 */
	protected Object createFieldReference(Object receiver) throws InvalidInputException {
		try {
			// Get receiver type
			TypeReference typeRef = (TypeReference) receiver;
			if (typeRef == null) {
				char[] name = this.sourceParser.compilationUnit.compilationResult.compilationUnit.getMainTypeName();
				if (name == null) {
					throw new InvalidInputException();
				}
				typeRef = new JavadocSingleTypeReference(name, 0, 0, 0);
			}
			// Create field
			JavadocFieldReference field = new JavadocFieldReference(this.identifierStack[0], this.identifierPositionStack[0]);
			field.receiver = typeRef;
			field.tagSourceStart = this.tagSourceStart;
			field.tagSourceEnd = this.tagSourceEnd;
			return field;
		}
		catch (ClassCastException ex) {
				throw new InvalidInputException();
		}
	}
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.compiler.parser.AbstractCommentParser#createMethodReference(java.lang.Object[])
	 */
	protected Object createMethodReference(Object receiver, List arguments) throws InvalidInputException {
		try {
			// Get receiver type
			TypeReference typeRef = (TypeReference) receiver;
			if (typeRef == null) {
				char[] name = this.sourceParser.compilationUnit.compilationResult.compilationUnit.getMainTypeName();
				if (name == null) {
					throw new InvalidInputException();
				}
				typeRef = new JavadocSingleTypeReference(name, 0, 0, 0);
			}
			// Decide whether we have a constructor or not
			char[][] receiverTokens = typeRef.getTypeName();
			char[] memberName = this.identifierStack[0];
			boolean isConstructor = CharOperation.equals(memberName, receiverTokens[receiverTokens.length-1]);
			// Create node
			if (arguments == null) {
				if (isConstructor) {
					JavadocAllocationExpression expr = new JavadocAllocationExpression(this.identifierPositionStack[0]);
					expr.type = typeRef;
					return expr;
				} else {
					JavadocMessageSend msg = new JavadocMessageSend(this.identifierStack[0], this.identifierPositionStack[0]);
					msg.receiver = typeRef;
					return msg;
				}
			} else {
				JavadocArgumentExpression[] expressions = new JavadocArgumentExpression[arguments.size()];
				arguments.toArray(expressions);
				if (isConstructor) {
					JavadocAllocationExpression alloc = new JavadocAllocationExpression(this.identifierPositionStack[0]);
					alloc.arguments = expressions;
					alloc.type = typeRef;
					return alloc;
				} else {
					JavadocMessageSend msg = new JavadocMessageSend(this.identifierStack[0], this.identifierPositionStack[0], expressions);
					msg.receiver = typeRef;
					return msg;
				}
			}
		}
		catch (ClassCastException ex) {
				throw new InvalidInputException();
		}
	}
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.compiler.parser.AbstractCommentParser#createReturnStatement()
	 */
	protected Object createReturnStatement() {
		return new JavadocReturnStatement(this.scanner.getCurrentTokenStartPosition(),
					this.scanner.getCurrentTokenEndPosition(),
					this.scanner.getRawTokenSourceEnd());
	}
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.compiler.parser.AbstractCommentParser#createSingleNameReference()
	 */
	protected Object createSingleNameReference() {
		JavadocSingleNameReference nameRef = new JavadocSingleNameReference(this.scanner.getCurrentIdentifierSource(),
				this.scanner.getCurrentTokenStartPosition(),
				this.scanner.getCurrentTokenEndPosition());
		nameRef.tagSourceStart = this.tagSourceStart;
		nameRef.tagSourceEnd = this.tagSourceEnd;
		return nameRef;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.compiler.parser.AbstractCommentParser#createTypeReference()
	 */
	protected Object createTypeReference(int primitiveToken) {
		TypeReference typeRef = null;
		int size = this.identifierLengthStack[this.identifierLengthPtr--];
		if (size == 1) { // Single Type ref
			typeRef = new JavadocSingleTypeReference(
						this.identifierStack[this.identifierPtr],
						this.identifierPositionStack[this.identifierPtr],
						this.tagSourceStart,
						this.tagSourceEnd);
		} else if (size > 1) { // Qualified Type ref
			char[][] tokens = new char[size][];
			System.arraycopy(this.identifierStack, this.identifierPtr - size + 1, tokens, 0, size);
			long[] positions = new long[size];
			System.arraycopy(this.identifierPositionStack, this.identifierPtr - size + 1, positions, 0, size);
			typeRef = new JavadocQualifiedTypeReference(tokens, positions, this.tagSourceStart, this.tagSourceEnd);
		}
		this.identifierPtr -= size;
		return typeRef;
	}
	protected void updateLineEnd() {
		while (this.index > (this.lineEnd+1)) {
			if (this.linePtr < this.lastLinePtr) {
				this.lineEnd = this.sourceParser.scanner.getLineEnd(++this.linePtr) - 1;
			} else {
				this.lineEnd = this.endComment;
				return;
			}
			this.lineStarted = false;
		}
	}
	/*
	 * Fill javadoc fields with information in ast nodes stack.
	 */
	protected void updateJavadoc() {
		
		// Set inherited flag
		this.javadoc.inherited = this.inherited;

		// Set return node if present
		if (this.returnStatement != null) {
			this.javadoc.returnStatement = (JavadocReturnStatement) this.returnStatement;
		}

		// If no nodes stored return
		if (this.astLengthPtr == -1) {
			return;
		}

		// Initialize arrays
		int[] sizes = new int[ORDERED_TAGS_NUMBER];
		for (int i=0; i<=this.astLengthPtr; i++) {
			sizes[i%ORDERED_TAGS_NUMBER] += this.astLengthStack[i];
		}
		this.javadoc.references = new Expression[sizes[SEE_TAG_EXPECTED_ORDER]];
		this.javadoc.thrownExceptions = new TypeReference[sizes[THROWS_TAG_EXPECTED_ORDER]];
		this.javadoc.parameters = new JavadocSingleNameReference[sizes[PARAM_TAG_EXPECTED_ORDER]];

		// Store nodes in arrays
		while (this.astLengthPtr >= 0) {
			int ptr = this.astLengthPtr % ORDERED_TAGS_NUMBER;
			// Starting with the stack top, so get references (eg. Expression) coming from @see declarations
			if (ptr == SEE_TAG_EXPECTED_ORDER) {
				int size = this.astLengthStack[this.astLengthPtr--];
				for (int i=0; i<size; i++) {
					this.javadoc.references[--sizes[ptr]] = (Expression) this.astStack[this.astPtr--];
				}
			}

			// Then continuing with class names (eg. TypeReference) coming from @throw/@exception declarations
			else if (ptr == THROWS_TAG_EXPECTED_ORDER) {
				int size = this.astLengthStack[this.astLengthPtr--];
				for (int i=0; i<size; i++) {
					this.javadoc.thrownExceptions[--sizes[ptr]] = (TypeReference) this.astStack[this.astPtr--];
				}
			}

			// Finally, finishing with parameters nales (ie. Argument) coming from @param declaration
			else if (ptr == PARAM_TAG_EXPECTED_ORDER) {
				int size = this.astLengthStack[this.astLengthPtr--];
				for (int i=0; i<size; i++) {
					this.javadoc.parameters[--sizes[ptr]] = (JavadocSingleNameReference) this.astStack[this.astPtr--];
				}
			}
		}
	}
}

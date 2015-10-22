package squeek.asmhelper;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.launchwrapper.LaunchClassLoader;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;

public class ASMHelper
{
	private static Boolean isCauldron = null;
	public static InsnComparator insnComparator = new InsnComparator();

	/**
	 * @return Whether or not Cauldron is loaded in the current environment.<br>
	 * <br>
	 * See: http://cauldron.minecraftforge.net/
	 */
	public static boolean isCauldron()
	{
		if (ASMHelper.isCauldron == null)
		{
			try
			{
				byte[] bytes = ((LaunchClassLoader) ASMHelper.class.getClassLoader()).getClassBytes("net.minecraftforge.cauldron.api.Cauldron");
				ASMHelper.isCauldron = bytes != null;
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}

		return ASMHelper.isCauldron;
	}

	/**
	 * Convert a byte array into a ClassNode.
	 */
	public static ClassNode readClassFromBytes(byte[] bytes)
	{
		ClassNode classNode = new ClassNode();
		ClassReader classReader = new ClassReader(bytes);
		classReader.accept(classNode, 0);
		return classNode;
	}

	/**
	 * Convert a ClassNode into a byte array.
	 * Attempts to resolve issues with resolving super classes in an obfuscated environment. 
	 * See {@link ObfRemappingClassWriter}.
	 */
	public static byte[] writeClassToBytes(ClassNode classNode)
	{
		ClassWriter writer = new ObfRemappingClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
		classNode.accept(writer);
		return writer.toByteArray();
	}

	/**
	 * Convert a ClassNode into a byte array.
	 * Will have issues with resolving super classes in an obfuscated environment.
	 */
	public static byte[] writeClassToBytesNoDeobf(ClassNode classNode)
	{
		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
		classNode.accept(writer);
		return writer.toByteArray();
	}

	/**
	 * Convert a ClassNode into a byte array.
	 * Does not re-compute frames. Primarily useful for avoiding ClassNotFoundExceptions in
	 * getCommonSuperClass that aren't solved by {@link #writeClassToBytes}/{@link ObfRemappingClassWriter}.
	 */
	public static byte[] writeClassToBytesNoDeobfSkipFrames(ClassNode classNode)
	{
		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		classNode.accept(writer);
		return writer.toByteArray();
	}

	/**
	 * @return Whether or not the instruction is a label or a line number.
	 */
	public static boolean isLabelOrLineNumber(AbstractInsnNode insn)
	{
		return insn.getType() == AbstractInsnNode.LABEL || insn.getType() == AbstractInsnNode.LINE;
	}

	/**
	 * @return The first instruction for which {@link AbstractInsnNode#getType()} == {@link type} (could be {@link firstInsnToCheck}). 
	 * If a matching instruction cannot be found, returns {@link null}.
	 */
	public static AbstractInsnNode getOrFindInstructionOfType(AbstractInsnNode firstInsnToCheck, int type)
	{
		return getOrFindInstructionOfType(firstInsnToCheck, type, false);
	}

	/**
	 * @return The first instruction for which {@link AbstractInsnNode#getType()} == {@link type} (could be {@link firstInsnToCheck}). 
	 * If {@link reverseDirection} is {@link true}, instructions will be traversed backwards (using getPrevious()).
	 * If a matching instruction cannot be found, returns {@link null}.
	 */
	public static AbstractInsnNode getOrFindInstructionOfType(AbstractInsnNode firstInsnToCheck, int type, boolean reverseDirection)
	{
		for (AbstractInsnNode instruction = firstInsnToCheck; instruction != null; instruction = reverseDirection ? instruction.getPrevious() : instruction.getNext())
		{
			if (instruction.getType() == type)
				return instruction;
		}
		return null;
	}

	/**
	 * @return The first instruction for which {@link AbstractInsnNode#getOpcode()} == {@link opcode} (could be {@link firstInsnToCheck}). 
	 * If a matching instruction cannot be found, returns {@link null}
	 */
	public static AbstractInsnNode getOrFindInstructionWithOpcode(AbstractInsnNode firstInsnToCheck, int opcode)
	{
		return getOrFindInstructionWithOpcode(firstInsnToCheck, opcode, false);
	}

	/**
	 * @return The first instruction for which {@link AbstractInsnNode#getOpcode()} == {@link opcode} (could be {@link firstInsnToCheck}). 
	 * If {@link reverseDirection} is {@link true}, instructions will be traversed backwards (using getPrevious()).
	 * If a matching instruction cannot be found, returns {@link null}.
	 */
	public static AbstractInsnNode getOrFindInstructionWithOpcode(AbstractInsnNode firstInsnToCheck, int opcode, boolean reverseDirection)
	{
		for (AbstractInsnNode instruction = firstInsnToCheck; instruction != null; instruction = reverseDirection ? instruction.getPrevious() : instruction.getNext())
		{
			if (instruction.getOpcode() == opcode)
				return instruction;
		}
		return null;
	}

	/**
	 * @return The first instruction for which {@link #isLabelOrLineNumber} == {@link true} (could be {@link firstInsnToCheck}). 
	 * If {@link reverseDirection} is {@link true}, instructions will be traversed backwards (using getPrevious()).
	 * If a matching instruction cannot be found, returns {@link null}.
	 */
	public static AbstractInsnNode getOrFindLabelOrLineNumber(AbstractInsnNode firstInsnToCheck)
	{
		return getOrFindInstruction(firstInsnToCheck, false);
	}

	/**
	 * @return The first instruction for which {@link #isLabelOrLineNumber} == {@link true} (could be {@link firstInsnToCheck}). 
	 * If a matching instruction cannot be found, returns {@link null}.
	 */
	public static AbstractInsnNode getOrFindLabelOrLineNumber(AbstractInsnNode firstInsnToCheck, boolean reverseDirection)
	{
		for (AbstractInsnNode instruction = firstInsnToCheck; instruction != null; instruction = reverseDirection ? instruction.getPrevious() : instruction.getNext())
		{
			if (isLabelOrLineNumber(instruction))
				return instruction;
		}
		return null;
	}

	/**
	 * @return The first instruction for which {@link #isLabelOrLineNumber} == {@link false} (could be {@link firstInsnToCheck}). 
	 * If a matching instruction cannot be found, returns {@link null}.
	 */
	public static AbstractInsnNode getOrFindInstruction(AbstractInsnNode firstInsnToCheck)
	{
		return getOrFindInstruction(firstInsnToCheck, false);
	}

	/**
	 * @return The first instruction for which {@link #isLabelOrLineNumber} == {@link false} (could be {@link firstInsnToCheck}). 
	 * If {@link reverseDirection} is {@link true}, instructions will be traversed backwards (using getPrevious()).
	 * If a matching instruction cannot be found, returns {@link null}.
	 */
	public static AbstractInsnNode getOrFindInstruction(AbstractInsnNode firstInsnToCheck, boolean reverseDirection)
	{
		for (AbstractInsnNode instruction = firstInsnToCheck; instruction != null; instruction = reverseDirection ? instruction.getPrevious() : instruction.getNext())
		{
			if (!isLabelOrLineNumber(instruction))
				return instruction;
		}
		return null;
	}

	/**
	 * @return The first instruction of the {@link method} for which {@link #isLabelOrLineNumber} == {@link false}. 
	 * If a matching instruction cannot be found, returns {@link null}.
	 */
	public static AbstractInsnNode findFirstInstruction(MethodNode method)
	{
		return getOrFindInstruction(method.instructions.getFirst());
	}

	/**
	 * @return The first instruction of the {@link method} for which {@link AbstractInsnNode#getOpcode()} == {@link opcode}. 
	 * If a matching instruction cannot be found, returns {@link null}.
	 */
	public static AbstractInsnNode findFirstInstructionWithOpcode(MethodNode method, int opcode)
	{
		return getOrFindInstructionWithOpcode(method.instructions.getFirst(), opcode);
	}

	/**
	 * @return The last instruction of the {@link method} for which {@link AbstractInsnNode#getOpcode()} == {@link opcode}. 
	 * If a matching instruction cannot be found, returns {@link null}.
	 */
	public static AbstractInsnNode findLastInstructionWithOpcode(MethodNode method, int opcode)
	{
		return getOrFindInstructionWithOpcode(method.instructions.getLast(), opcode, true);
	}

	/**
	 * @return The next instruction after {@link instruction} for which {@link #isLabelOrLineNumber} == {@link false} 
	 * (excluding {@link instruction). 
	 * If a matching instruction cannot be found, returns {@link null}.
	 */
	public static AbstractInsnNode findNextInstruction(AbstractInsnNode instruction)
	{
		return getOrFindInstruction(instruction.getNext());
	}

	/**
	 * @return The next instruction after {@link instruction} for which {@link AbstractInsnNode#getOpcode()} == {@link opcode} 
	 * (excluding {@link instruction). 
	 * If a matching instruction cannot be found, returns {@link null}.
	 */
	public static AbstractInsnNode findNextInstructionWithOpcode(AbstractInsnNode instruction, int opcode)
	{
		return getOrFindInstructionWithOpcode(instruction.getNext(), opcode);
	}

	/**
	 * @return The next instruction after {@link instruction} for which {@link #isLabelOrLineNumber} == {@link true} 
	 * (excluding {@link instruction). 
	 * If a matching instruction cannot be found, returns {@link null}.
	 */
	public static AbstractInsnNode findNextLabelOrLineNumber(AbstractInsnNode instruction)
	{
		return getOrFindLabelOrLineNumber(instruction.getNext());
	}

	/**
	 * @return The previous instruction before {@link instruction} for which {@link #isLabelOrLineNumber} == {@link false} 
	 * (excluding {@link instruction). 
	 * If a matching instruction cannot be found, returns {@link null}.
	 */
	public static AbstractInsnNode findPreviousInstruction(AbstractInsnNode instruction)
	{
		return getOrFindInstruction(instruction.getPrevious(), true);
	}

	/**
	 * @return The previous instruction before {@link instruction} for which {@link AbstractInsnNode#getOpcode()} == {@link opcode} 
	 * (excluding {@link instruction). 
	 * If a matching instruction cannot be found, returns {@link null}.
	 */
	public static AbstractInsnNode findPreviousInstructionWithOpcode(AbstractInsnNode instruction, int opcode)
	{
		return getOrFindInstructionWithOpcode(instruction.getPrevious(), opcode, true);
	}

	/**
	 * @return The previous instruction before {@link instruction} for which {@link #isLabelOrLineNumber} == {@link true} 
	 * (excluding {@link instruction). 
	 * If a matching instruction cannot be found, returns {@link null}.
	 */
	public static AbstractInsnNode findPreviousLabelOrLineNumber(AbstractInsnNode instruction)
	{
		return getOrFindLabelOrLineNumber(instruction.getPrevious(), true);
	}

	/**
	 * @return The method of the class that has both a matching {@link methodName} and {@link methodDesc}.
	 * If no matching method is found, returns {@link null}.
	 */
	public static MethodNode findMethodNodeOfClass(ClassNode classNode, String methodName, String methodDesc)
	{
		for (MethodNode method : classNode.methods)
		{
			if (method.name.equals(methodName) && (methodDesc == null || method.desc.equals(methodDesc)))
			{
				return method;
			}
		}
		return null;
	}

	public static MethodNode findMethodNodeOfClass(ClassNode classNode, String methodName, String methodName2, String methodDesc)
	{
		for (MethodNode method : classNode.methods)
		{
			if ((method.name.equals(methodName) || method.name.equals(methodName2)) && (methodDesc == null || method.desc.equals(methodDesc)))
			{
				return method;
			}
		}
		return null;
	}

	/**
	 * Useful for defining the end label for ASM-inserted local variables.
	 * 
	 * @return The last label of the method (usually after the RETURN instruction).
	 * If no matching {@link LabelNode} is found, returns {@link null}.
	 */
	public static LabelNode findEndLabel(MethodNode method)
	{
		for (AbstractInsnNode instruction = method.instructions.getLast(); instruction != null; instruction = instruction.getPrevious())
		{
			if (instruction instanceof LabelNode)
				return (LabelNode) instruction;
		}
		return null;
	}

	/**
	 * Remove instructions from {@link insnList} starting with {@link startInvlusive} 
	 * up until reaching {@link endNotInclusive} ({@link endNotInclusive} will not be removed).
	 * 
	 * @return The number of instructions removed
	 */
	public static int removeFromInsnListUntil(InsnList insnList, AbstractInsnNode startInclusive, AbstractInsnNode endNotInclusive)
	{
		AbstractInsnNode insnToRemove = startInclusive;
		int numDeleted = 0;
		while (insnToRemove != null && insnToRemove != endNotInclusive)
		{
			numDeleted++;
			insnToRemove = insnToRemove.getNext();
			insnList.remove(insnToRemove.getPrevious());
		}
		return numDeleted;
	}

	/**
	 * Note: Does not move the instruction, but rather gets the instruction a certain
	 * distance away from {@link start}.<br>
	 * <br>
	 * 
	 * <b>Example:</b>
	 * <pre>
	 * {@code
	 * // fifthInsn is pointing to the 5th instruction of insnList
	 * AbstractInsnNode thirdInsn = ASMHelper.move(fifthInsn, -2);
	 * AbstractInsnNode eightInsn = ASMHelper.move(fifthInsn, 3);
	 * }
	 * </pre>
	 * 
	 * @param start The instruction to move from
	 * @param distance The distance to move (can be positive or negative)
	 * @return The instruction {@link distance} away from the {@link start} instruction
	 */
	public static AbstractInsnNode move(final AbstractInsnNode start, int distance)
	{
		AbstractInsnNode movedTo = start;
		for (int i = 0; i < Math.abs(distance) && movedTo != null; i++)
		{
			movedTo = distance > 0 ? movedTo.getNext() : movedTo.getPrevious();
		}
		return movedTo;
	}

	/**
	 * Convenience method for accessing {@link InsnComparator#areInsnsEqual}
	 */
	public static boolean instructionsMatch(AbstractInsnNode first, AbstractInsnNode second)
	{
		return insnComparator.areInsnsEqual(first, second);
	}

	/**
	 * @return Whether or not the pattern in {@link checkFor} matches starting at {@link checkAgainst}
	 */
	public static boolean patternMatches(InsnList checkFor, AbstractInsnNode checkAgainst)
	{
		return checkForPatternAt(checkFor, checkAgainst).getFirst() != null;
	}

	/**
	 * Checks whether or not the pattern in {@link checkFor} matches, starting at {@link checkAgainst}.
	 * 
	 * @return All of the instructions that were matched by the {@link checkFor} pattern.
	 * If the pattern was not found, returns an empty {@link InsnList}.<br>
	 * <br>
	 * Note: If the pattern was matched, the size of the returned {@link InsnList} will be >= {@link checkFor}.size().
	 */
	public static InsnList checkForPatternAt(InsnList checkFor, AbstractInsnNode checkAgainst)
	{
		InsnList foundInsnList = new InsnList();
		boolean firstNeedleFound = false;
		for (AbstractInsnNode lookFor = checkFor.getFirst(); lookFor != null;)
		{
			if (checkAgainst == null)
				return new InsnList();

			if (isLabelOrLineNumber(lookFor))
			{
				lookFor = lookFor.getNext();
				continue;
			}

			if (isLabelOrLineNumber(checkAgainst))
			{
				if (firstNeedleFound)
					foundInsnList.add(checkAgainst);
				checkAgainst = checkAgainst.getNext();
				continue;
			}

			if (!instructionsMatch(lookFor, checkAgainst))
				return new InsnList();

			foundInsnList.add(checkAgainst);
			lookFor = lookFor.getNext();
			checkAgainst = checkAgainst.getNext();
			firstNeedleFound = true;
		}
		return foundInsnList;
	}

	/**
	 * Searches for the pattern in {@link needle}, starting at {@link haystackStart}.
	 * 
	 * @return All of the instructions that were matched by the pattern.
	 * If the pattern was not found, returns an empty {@link InsnList}.<br>
	 * <br>
	 * Note: If the pattern was matched, the size of the returned {@link InsnList} will be >= {@link checkFor}.size().
	 */
	public static InsnList findAndGetFoundInsnList(AbstractInsnNode haystackStart, InsnList needle)
	{
		int needleStartOpcode = needle.getFirst().getOpcode();
		AbstractInsnNode checkAgainstStart = getOrFindInstructionWithOpcode(haystackStart, needleStartOpcode);
		while (checkAgainstStart != null)
		{
			InsnList found = checkForPatternAt(needle, checkAgainstStart);

			if (found.getFirst() != null)
				return found;

			checkAgainstStart = findNextInstructionWithOpcode(checkAgainstStart, needleStartOpcode);
		}
		return new InsnList();
	}

	/**
	 * Searches for the pattern in {@link needle} within {@link haystack}.
	 * 
	 * @return The first instruction of the matched pattern.
	 * If the pattern was not found, returns an empty {@link InsnList}.
	 */
	public static AbstractInsnNode find(InsnList haystack, InsnList needle)
	{
		return find(haystack.getFirst(), needle);
	}

	/**
	 * Searches for the pattern in {@link needle}, starting at {@link haystackStart}.
	 * 
	 * @return The first instruction of the matched pattern.
	 * If the pattern was not found, returns @{link null}.
	 */
	public static AbstractInsnNode find(AbstractInsnNode haystackStart, InsnList needle)
	{
		if (needle.getFirst() == null)
			return null;

		InsnList found = findAndGetFoundInsnList(haystackStart, needle);
		return found.getFirst();
	}

	/**
	 * Searches for an instruction matching {@link needle} within {@link haystack}.
	 * 
	 * @return The matching instruction.
	 * If a matching instruction was not found, returns @{link null}.
	 */
	public static AbstractInsnNode find(InsnList haystack, AbstractInsnNode needle)
	{
		return find(haystack.getFirst(), needle);
	}

	/**
	 * Searches for an instruction matching {@link needle}, starting at {@link haystackStart}.
	 * 
	 * @return The matching instruction.
	 * If a matching instruction was not found, returns @{link null}.
	 */
	public static AbstractInsnNode find(AbstractInsnNode haystackStart, AbstractInsnNode needle)
	{
		InsnList insnList = new InsnList();
		insnList.add(needle);
		return find(haystackStart, insnList);
	}

	/**
	 * Searches for the pattern in {@link needle} within {@link haystack} and replaces it with {@link replacement}.
	 * 
	 * @return The instruction after the replacement.
	 * If the pattern was not found, returns @{link null}.
	 */
	public static AbstractInsnNode findAndReplace(InsnList haystack, InsnList needle, InsnList replacement)
	{
		return findAndReplace(haystack, needle, replacement, haystack.getFirst());
	}

	/**
	 * Searches for the pattern in {@link needle} within {@link haystack} (starting at {@link haystackStart})
	 * and replaces it with {@link replacement}.
	 * 
	 * @return The instruction after the replacement.
	 * If the pattern was not found, returns @{link null}.
	 */
	public static AbstractInsnNode findAndReplace(InsnList haystack, InsnList needle, InsnList replacement, AbstractInsnNode haystackStart)
	{
		InsnList found = findAndGetFoundInsnList(haystackStart, needle);
		if (found.getFirst() != null)
		{
			haystack.insertBefore(found.getFirst(), replacement);
			AbstractInsnNode afterNeedle = found.getLast().getNext();
			removeFromInsnListUntil(haystack, found.getFirst(), afterNeedle);
			return afterNeedle;
		}
		return null;
	}

	/**
	 * Searches for all instances of the pattern in {@link needle} within {@link haystack} 
	 * and replaces them with {@link replacement}.
	 * 
	 * @return The number of replacements made.
	 */
	public static int findAndReplaceAll(InsnList haystack, InsnList needle, InsnList replacement)
	{
		return findAndReplaceAll(haystack, needle, replacement, haystack.getFirst());
	}

	/**
	 * Searches for all instances of the pattern in {@link needle} within {@link haystack} 
	 * (starting at {@link haystackStart}) and replaces them with {@link replacement}.
	 * 
	 * @return The number of replacements made.
	 */
	public static int findAndReplaceAll(InsnList haystack, InsnList needle, InsnList replacement, AbstractInsnNode haystackStart)
	{
		int numReplaced = 0;
		while ((haystackStart = findAndReplace(haystack, needle, replacement, haystackStart)) != null)
		{
			numReplaced++;
		}
		return numReplaced;
	}

	/**
	 * Clones an instruction list, remapping labels in the process.
	 * 
	 * @return The cloned {@link InsnList}
	 */
	public static InsnList cloneInsnList(InsnList source)
	{
		InsnList clone = new InsnList();

		// used to map the old labels to their cloned counterpart
		Map<LabelNode, LabelNode> labelMap = new HashMap<LabelNode, LabelNode>();

		// build the label map
		for (AbstractInsnNode instruction = source.getFirst(); instruction != null; instruction = instruction.getNext())
		{
			if (instruction instanceof LabelNode)
			{
				labelMap.put(((LabelNode) instruction), new LabelNode());
			}
		}

		for (AbstractInsnNode instruction = source.getFirst(); instruction != null; instruction = instruction.getNext())
		{
			clone.add(instruction.clone(labelMap));
		}

		return clone;
	}

	/**
	 * @return The local variable of the method that has both a matching {@link varName} and {@link varDesc}.
	 * If no matching local variable is found, returns {@link null}.
	 */
	public static LocalVariableNode findLocalVariableOfMethod(MethodNode method, String varName, String varDesc)
	{
		for (LocalVariableNode localVar : method.localVariables)
		{
			if (localVar.name.equals(varName) && localVar.desc.equals(varDesc))
			{
				return localVar;
			}
		}
		return null;
	}

	private static Printer printer = new Textifier();
	private static TraceMethodVisitor methodprinter = new TraceMethodVisitor(printer);

	/**
	 * @return {@link insnList} as a string.<br>
	 * <br>
	 * <b>Example output:</b><br>
	 * <pre>
	 *    ALOAD 0
	 *    GETFIELD net/minecraft/util/FoodStats.foodLevel : I
	 *    ISTORE 3
	 *    ALOAD 0
	 * </pre>
	 */
	public static String getInsnListAsString(InsnList insnList)
	{
		insnList.accept(methodprinter);
		StringWriter sw = new StringWriter();
		printer.print(new PrintWriter(sw));
		printer.getText().clear();
		return sw.toString();
	}

	/**
	 * @return {@link method} as a string.
	 */
	public static String getMethodAsString(MethodNode method)
	{
		method.accept(methodprinter);
		StringWriter sw = new StringWriter();
		printer.print(new PrintWriter(sw));
		printer.getText().clear();
		return sw.toString();
	}
}

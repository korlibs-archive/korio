package com.soywiz.korio.compression.util

// @TODO: Optimize for instance reusal and to not require one node per item, but using arrays
class HuffmanTree(val root: Node, val symbolLimit: Int) {
	class Node(val value: Int, val len: Int, val left: Node?, val right: Node?) {
		val isLeaf get() = this.len != 0

		companion object {
			fun leaf(value: Int, len: Int) = Node(value, len, null, null)
			fun int(left: Node, right: Node) = Node(-1, 0, left, right)
		}
	}

	data class Result(var value: Int, var bitcode: Int, var bitcount: Int)

	fun sreadOne(reader: BitReader, out: Result = Result(0, 0, 0)): Result {
		//console.log('-------------');
		var node: Node? = this.root
		var bitcount = 0
		var bitcode = 0
		do {
			val bbit = reader.readBits(1)
			val bit = (bbit != 0)
			bitcode = bitcode or (bbit shl bitcount)
			bitcount++
			//console.log('bit', bit);
			node = if (bit) node!!.right else node!!.left
			//console.info(node);
		} while (node != null && node.len == 0)
		if (node == null) error("NODE = NULL")
		return out.apply {
			this.value = node.value
			this.bitcode = bitcode
			this.bitcount = bitcount
		}
	}

	inline fun sreadOneValue(reader: BitReader, tempResult: Result) = sreadOne(reader, tempResult).value

	companion object {
		fun fromLengths(codeLengths: IntArray): HuffmanTree {
			var nodes = arrayListOf<Node>()
			for (i in (codeLengths.max() ?: 0) downTo 1) {
				val newNodes = arrayListOf<Node>()
				for (j in 0 until codeLengths.size) if (codeLengths[j] == i) newNodes.add(
					Node.leaf(j, i)
				)
				for (j in 0 until nodes.size step 2) newNodes.add(
					Node.int(nodes[j], nodes[j + 1])
				)
				nodes = newNodes
				if (nodes.size % 2 != 0) error("This canonical code does not represent a Huffman code tree: ${nodes.size}")
			}
			if (nodes.size != 2) error("This canonical code does not represent a Huffman code tree")
			return HuffmanTree(Node.int(nodes[0], nodes[1]), codeLengths.size)
		}
	}
}

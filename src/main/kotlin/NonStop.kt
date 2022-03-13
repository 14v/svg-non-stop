import org.w3c.dom.NamedNodeMap
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.File
import java.net.URI
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

/**
 * Utility for repairing SVG for compatibility with Android build tools vector drawable
 * conversion.
 */
object NonStop {

    /**
     * Whether to log verbosely when processing.
     */
    var optionVerbose = false

    /**
     * Modifies the given XML [rootNodes] to repair missing stop info so the SVG can be
     * converted to an Android vector asset by the Android build tools.
     * @return whether any changes were made.
     */
    fun processSvg(rootNodes: NodeList): Boolean {
        val defsNode = rootNodes.findNode("defs")
        if (defsNode == null) {
            println("No 'defs' node found, possibly this file does not contain gradients.")
            return false
        }

        val gradientNodes = defsNode.childNodes
        val gradientStopsMap = mutableMapOf<String, List<Node>>()
        val targetGradientsMap = mutableMapOf<String, MutableList<Node>>()

        gradientNodes.forEach {
            // Skipping whitespace nodes
            if (it.nodeName == "#text") return@forEach

            logv("Processing gradient node ${it.nodeName}")

            val xlinkedId = it.getAttribute("xlink:href")?.removePrefix("#")
            if (xlinkedId != null) {
                // Links to gradients
                if (targetGradientsMap[xlinkedId] == null) targetGradientsMap[xlinkedId] = mutableListOf()
                targetGradientsMap[xlinkedId]!!.add(it)
                logv("Found linked gradient, id $xlinkedId")
            } else if (it.isGradient()) {
                extractStops(it, gradientStopsMap)
            }
        }

        if (gradientStopsMap.isEmpty()) {
            println("Gradient stops not found in defs, searching in g(roups)...")
            val gNode = rootNodes.findNode("g")
            if (gNode == null) {
                println("No 'g' nodes - stops not found.")
                return false
            }

            logv("Found 'g' node...")
            gNode.childNodes.forEachSearchGradients {
                extractStops(it, gradientStopsMap)
            }

        }

        if (gradientStopsMap.isEmpty()) {
            println("Gradient stops not found in gs.")
            return false
        }

        println("Inserting stops into target gradients.")
        targetGradientsMap.forEach { targetGradientsMapEntry ->
            val targetGradientId = targetGradientsMapEntry.key
            val stops = gradientStopsMap[targetGradientId] ?: return@forEach
            val targetGradients = targetGradientsMapEntry.value
            logv("Inserting stops for id $targetGradientId, ${targetGradients.count()} gradient(s) count")
            targetGradients.forEach { targetGradient ->
                stops.forEachIndexed { stopNum, stop ->
                    logv("Inserting stop #$stopNum for target gradient ${targetGradient.getId()}")
                    // appendChild will move node if attached to document already, cloning then
                    targetGradient.appendChild(stop.cloneNode(false))
                }
            }
        }

        return true
    }

    /**
     * Process linear gradients with <stop> tags
     */
    private fun extractStops(linearGradient: Node, gradientStopsMap: MutableMap<String, List<Node>>) {
        val stopNodes = mutableListOf<Node>()
        linearGradient.childNodes.forEach { stopNode -> if (stopNode.nodeName == "stop") stopNodes.add(stopNode) }
        val id = linearGradient.getId()
        logv("Found linear gradient, id $id, has ${stopNodes.count()} stops")

        if (stopNodes.isEmpty()) return
        gradientStopsMap[id] = stopNodes
    }

    private fun NodeList.forEach(action: (Node) -> Unit) {
        for (i in 0 until this.length) action.invoke(this.item(i))
    }

    // Dumb recursive, may need performance update on big files
    private fun NodeList.forEachSearchGradients(action: (Node) -> Unit) {
        for (i in 0 until this.length) {
            val node = this.item(i)
            val childNodes = node.childNodes

            if (childNodes.length > 0 && !node.isGradient()) {
                logv("Node contains nested groups, g id " + node.getId())
                childNodes.forEachSearchGradients(action)
            } else if (node.isGradient()) {
                action.invoke(node)
            }
        }
    }

    private fun NodeList.findNode(name: String): Node? {
        for (i in 0 until this.length) {
            val node = this.item(i)
            if (node.nodeName == name) return node
        }
        return null
    }

    private fun NamedNodeMap.hasNode(name: String): Boolean {
        for (i in 0 until this.length) {
            val node = this.item(i)
            if (node.nodeName == name) return true
        }
        return false
    }

    private fun Node.getId(): String = getAttribute("id")!!

    private fun Node.getAttribute(attrName: String): String? {
        return this.attributes.getNamedItem(attrName)?.nodeValue
    }

    private fun Node.isGradient(): Boolean = nodeName == "linearGradient" || nodeName == "radialGradient"

    private fun logv(message: String) {
        if (optionVerbose) println(message)
    }
}


fun main(args: Array<String>) {
    var filename: String? = null
    var optionForceOverwrite = false
    args.forEach {
        when (it) {
            "-f" -> {
                optionForceOverwrite = true
                println("Option Force overwrite is on.")
            }
            "-v" -> {
                NonStop.optionVerbose = true
                println("Option Verbose is on.")
            }
            else -> filename = it
        }
    }
    if (filename == null) {
        println("Pass target SVG file name as parameter.\n" +
                "Add -f to force overwrite target _nonstop.svg file, -v to have verbose output.")
        return
    }

    val documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
    val document = documentBuilder.parse(filename)
    val documentURI = document.documentURI

    val nonstopDocumentURI = documentURI.replace(".svg", "_nonstop.svg")
    val nonstopFile = File(URI.create(nonstopDocumentURI))
    if (nonstopFile.exists() && !optionForceOverwrite) {
        println("Target file ${nonstopFile.name} already exists, exiting. Use -f option to force overwrite.")
        return
    }

    println("Parsed file $documentURI")
    val nodes = document.documentElement.childNodes
    println("Document contains ${nodes.length} nodes.")

    val resultOk = NonStop.processSvg(nodes)

    if (!resultOk) {
        println("Errors occurred, exiting.")
        return
    }

    println("Processed document.")
    val domSource = DOMSource(document)
    val streamResult = StreamResult(nonstopFile)
    TransformerFactory.newInstance().newTransformer().transform(domSource, streamResult)
    println("Saved new file $nonstopDocumentURI successfully.")
}
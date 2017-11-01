package cyclients.saxon.adapter;

import cysystem.clientsmanager.ClientsFactory;
import cysystem.clientsmanager.CyGUI;
import cysystem.clientsmanager.OSValidator;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.XQueryCompiler;
import net.sf.saxon.s9api.XQueryEvaluator;
import net.sf.saxon.s9api.XQueryExecutable;

public class SaxonAdapter extends ClientsFactory {
	private String currentTimeLog;
	private Processor proc;
	private XQueryCompiler comp;
	private boolean schemaValidationStrict;

	public void initialize(CyGUI gui, int clientID) {
		this.dbgui = gui;
		this.schemaValidationStrict = false;
		this.currentTimeLog = null;
	}

	public void execute(int clientID, String command) {
		if (this.dbgui == null) {
			System.out
					.println("Error! The client parser is not initialized properly. The handle to CyDIW GUI is not initialized.");
			return;
		}

		String workspacePath = this.dbgui.getClientsManager()
				.getClientWorkspacePath(clientID).trim();
		if ((workspacePath == null) || (workspacePath.isEmpty())) {
			System.out
					.println("Warning: The workspace path of the Saxon client system is not set. The query results will be stored into the current working path.");
			this.dbgui
					.addConsoleMessage("Warning: The workspace path of the Saxon client system is not set. The query results will be stored into the current working path.");
		} else if (OSValidator.isWindows()) {
			workspacePath = workspacePath + "\\";
		} else {
			workspacePath = workspacePath + "/";
		}
		String resultFileName = null;
		String xqueryString;
		if (command.indexOf(":>") != -1) {
			resultFileName = command.substring(0, command.indexOf(":>")).trim();
			xqueryString = command.substring(command.indexOf(":>") + 2);
		} else {
			xqueryString = command;
		}

		String[] cmd = xqueryString.trim().split(" ");
		if(cmd[0].startsWith("$$"))
		{
			xqueryString = dbgui.getVariableValue(cmd[0].substring(2));
		}
		if (cmd[0].equalsIgnoreCase("setStrictSchemaValidation")) {
			if (cmd.length >= 2) {
				if (cmd[1].equalsIgnoreCase("true")) {
					this.schemaValidationStrict = true;
					this.dbgui
							.addOutputPlainText("The STRICT schema validation mode of Saxon is now ON.");
					this.dbgui.addOutputBlankLine();
					this.dbgui.setFlag2On();
					this.currentTimeLog = "0";  // In order to hide the null poiter
				} else if (cmd[1].equalsIgnoreCase("false")) {
					this.schemaValidationStrict = false;
					this.dbgui
							.addOutputPlainText("The STRICT schema validation mode of Saxon is now OFF.");
					this.dbgui.addOutputBlankLine();
					this.dbgui.setFlag2Off();
					this.currentTimeLog = "0";
				}
			} else {
				this.dbgui
						.addOutputPlainText("Could not execute the command \"$"
								+ this.dbgui.getClientsManager()
										.getClientPrefix(clientID) + ":>"
								+ command + "\"");
				this.dbgui.addOutputBlankLine();
				this.dbgui
						.addConsoleMessage("Error: The syntax of the \"setStrictValidation\" command is incorrect.");
			}
		}

		else
			try {
				if (this.schemaValidationStrict) {
					this.proc = new Processor(true);

					this.proc
							.setConfigurationProperty(
									"http://saxon.sf.net/feature/schema-validation-mode",
									"strict");
					this.comp = this.proc.newXQueryCompiler();

					this.comp.setSchemaAware(true);
				} else {
					this.proc = new Processor(false);
					this.comp = this.proc.newXQueryCompiler();
				}

				Serializer out = new Serializer();
				out.setOutputProperty(Serializer.Property.METHOD, "xml");
				out.setOutputProperty(Serializer.Property.INDENT, "yes");
				out.setOutputProperty(Serializer.Property.OMIT_XML_DECLARATION,
						"yes");

				OutputStream resultStream = null;
				ByteArrayOutputStream baOS = null;

				if (resultFileName != null) {
					File resultFile = new File(workspacePath + resultFileName);
					resultStream = new FileOutputStream(resultFile);
					resultStream.write(new String("<Root>").getBytes());
					out.setOutputStream(resultStream);
				} else {
					baOS = new ByteArrayOutputStream();
					out.setOutputStream(baOS);
				}

				long startTime = System.currentTimeMillis();

				XQueryExecutable exp = this.comp.compile(xqueryString);
				XQueryEvaluator eval = exp.load();
				eval.run(out);

				long compile_executionTime = System.currentTimeMillis()
						- startTime;
				this.currentTimeLog = Long.toString(compile_executionTime);

				if (resultFileName != null) {
					resultStream.write(new String("</Root>").getBytes());
					resultStream.flush();
					resultStream.close();
				} else {
					this.dbgui.addOutput(baOS.toString());
					this.dbgui.addOutputBlankLine();
				}
			} catch (Exception x) {
				this.dbgui
						.addOutputPlainText("Could not execute the command \"$"
								+ this.dbgui.getClientsManager()
										.getClientPrefix(clientID) + ":>"
								+ command + "\"");
				this.dbgui.addOutputBlankLine();
				this.dbgui.addConsoleMessage("Exception Caught: " + x);
			}
	}

	public String getTimeLogData() {
		return this.currentTimeLog;
	}

	public static void main(String[] args) throws Exception {
		for (int i = 0; i < 10; i++) {
			String xqueryString = "for $b in doc(\"E:/auctions10.xml\")/site/open_auctions/open_auction return <increase> { $b/bidder[1]/increase/text() } </increase>";

			Serializer out = new Serializer();
			out.setOutputProperty(Serializer.Property.METHOD, "xml");
			out.setOutputProperty(Serializer.Property.INDENT, "yes");
			out.setOutputProperty(Serializer.Property.OMIT_XML_DECLARATION,
					"yes");

			ByteArrayOutputStream baOS = new ByteArrayOutputStream();
			out.setOutputStream(baOS);

			Processor proc = new Processor(false);
			XQueryCompiler comp = proc.newXQueryCompiler();

			long startTime = System.currentTimeMillis();

			XQueryExecutable exp = comp.compile(xqueryString);

			XQueryEvaluator eval = exp.load();
			eval.run(out);

			long endTime = System.currentTimeMillis() - startTime;
			System.out.println("Execution time: " + endTime);
		}
	}
}
//test end of file

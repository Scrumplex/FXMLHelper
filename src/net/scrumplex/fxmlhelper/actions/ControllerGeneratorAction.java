package net.scrumplex.fxmlhelper.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.psi.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

class ControllerGeneratorAction extends AnAction {

    private final String accessLevel;

    public ControllerGeneratorAction() {
        super();
        this.accessLevel = "private";
    }

    ControllerGeneratorAction(String accessLevel) {
        super();
        this.accessLevel = accessLevel;
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        if (psiFile != null) {
            PsiClass psiClass = JavaDirectoryService.getInstance().createClass(psiFile.getContainingDirectory(), getClassName(psiFile));
            WriteCommandAction.runWriteCommandAction(e.getProject(), () -> {
                try {
                    generateFields(psiFile, psiClass);
                } catch (IOException | SAXException | ParserConfigurationException e1) {
                    e1.printStackTrace();
                }
            });
        }
    }

    @Override
    public void update(AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(false);
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        if (psiFile != null) {
            if (psiFile.getOriginalFile().getName().endsWith(".fxml")) {
                e.getPresentation().setEnabledAndVisible(true);
            }
        }
    }

    private String getClassName(PsiFile file) {
        String psiName = file.getName();
        psiName = psiName.substring(0, psiName.length() - 5); //sample.fxml -> sample
        psiName = psiName.substring(0, 1).toUpperCase() + psiName.substring(1); //sample -> Sample
        psiName = psiName + "Controller"; //Sample -> SampleController
        return psiName;
    }

    private void generateFields(PsiFile fxmlFile, PsiClass classFile) throws IOException, SAXException, ParserConfigurationException {
        String str = fxmlFile.getText();
        JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(classFile.getProject());

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputSource is = new InputSource(new StringReader(str));
        Document doc = builder.parse(is);
        List<Node> children = findChildrenNodes(doc);

        for (Node n : children) {
            if (n instanceof Element) {
                Element e = (Element) n;
                if (e.hasAttribute("fx:id")) {

                    PsiField f = psiFacade.getElementFactory().createFieldFromText("@FXML " + accessLevel + " " + e.getTagName() + " " + e.getAttribute("fx:id") + ";", psiFacade.findPackage(""));
                    classFile.add(f);
                }
            }
        }
    }

    private List<Node> findChildrenNodes(Node node) {
        List<Node> nodes = new ArrayList<>();
        for (int i = 0; i < node.getChildNodes().getLength(); i++) {
            Node n = node.getChildNodes().item(i);
            nodes.add(n);
            if (n.hasChildNodes()) {
                nodes.addAll(findChildrenNodes(n));
            }
        }
        return nodes;
    }
}

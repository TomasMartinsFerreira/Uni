package pt.tecnico.blockchainist.node;

import java.io.InputStream;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;

public class InitKeys {

    private String org;
    private Map<String, PublicKey> orgClients;

    public InitKeys(String org) {
        this.org = org;
        this.orgClients = new HashMap<>();
        try {
            initOrgClients();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao inicializar chaves da organização: " + org, e);
        }
    }

    private void initOrgClients() throws Exception {
        if ("OrgA".equals(this.org)) {
            loadAndPut("BC", "seq/BC.pub");
            loadAndPut("Alice", "seq/Alice.pub");
            loadAndPut("Bob", "seq/Bob.pub");
            loadAndPut("Charlie", "seq/Charlie.pub");
        } else if ("OrgB".equals(this.org)) {
            loadAndPut("David", "seq/David.pub");
            loadAndPut("Emma", "seq/Emma.pub");
            loadAndPut("Fred", "seq/Fred.pub");
        } else if ("OrgC".equals(this.org)) {
            loadAndPut("Ginger", "seq/Ginger.pub");
            loadAndPut("Henry", "seq/Henry.pub");
            loadAndPut("Iris", "seq/Iris.pub");
        }
    }

    private void loadAndPut(String name, String fileName) throws Exception {
        orgClients.put(name, loadPublicKey(fileName));
    }


    public static PublicKey loadPublicKey(String resourcePath) throws Exception {
        byte[] keyBytes = readResource(resourcePath);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(spec);
    }

    private static byte[] readResource(String path) throws Exception {
        try (InputStream is = InitKeys.class.getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                throw new IllegalArgumentException("File not found in resources: " + path);
            }
            return is.readAllBytes();
        }
    }


    public boolean verifySignature(String entityName, byte[] data, byte[] signatureBytes) {
        PublicKey pubKey = orgClients.get(entityName);
        if (pubKey == null) {
            return false;
        }
        try {
            java.security.Signature sig = java.security.Signature.getInstance("SHA256withRSA");
            sig.initVerify(pubKey);
            sig.update(data);
            return sig.verify(signatureBytes);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isOrgUser(String userId) {
        return orgClients.containsKey(userId);
    }
}

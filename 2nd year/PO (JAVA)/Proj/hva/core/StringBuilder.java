package hva.core;

import java.util.*;

class StringBuilder{
    //pode haver um decorator pra dar a opcao de sortear ou nao
    static String[] getString(Object[] _children){
        Arrays.sort(_children); 
        String[] childrenStrings = new String[_children.length];
        int i = 0;
        for (Object child : _children) {    
            childrenStrings[i] = child.toString();
            i++;
        }
        return childrenStrings;
    }

}

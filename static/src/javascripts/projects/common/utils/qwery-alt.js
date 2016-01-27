define(function() {
    var cssRE = /^(?:[a-zA-Z_]|(-[a-zA-Z0-9_]))[a-zA-Z0-9_-]*$/;

    function normalizeRoot(root) {
        return root === undefined ? document :
            root instanceof Element ? root :
            root instanceof String ? get(root) :
            document;
    }

    function getAll(selector, root) {
        root = normalizeRoot(root);
        if( cssRE.test(selector) ) {
            return getByTag(selector, root);
        }
        if( cssRE.test(selector.substring(1)) ) {
            switch( selector.charAt(0) ) {
                case '#': return [getById(selector)];
                case '.': return getByClass(selector, root);
            }
        }
        return root.querySelectorAll(selector);
    }

    function get(selector, root) {
        root = normalizeRoot(root);
        if(
            selector.charAt(0) === '#' &&
            cssRE.test(selector.substring(1))
        ) {
            return getById(selector);
        }
        return root.querySelector(selector);
    }

    function getById(id) {
        return document.getElementById(id);
    }

    function getByTag(tn, root) {
        root = normalizeRoot(root);
        return root.getElementsByTagName(selector);
    }

    function getByClass(cn, root) {
        root = normalizeRoot(root);
        return root.getElementsByClassName(cn);
    }

    return getAll;
});

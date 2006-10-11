/*
 * Copyright (C) 2006, The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


/**
 * This class allows you to create a composite that is populated from
 * a message pattern and inserts controls at the appropriate places.
 * For example, say that the message <code>MyMsg.repeatTimes</code> is
 * defined as the following:
 * <pre>
 * MyMsg.repeatTimes = "Repeat: {0} times";
 * </pre>
 * and you want to replace "{0}" with an input field or perhaps a
 * drop-down menu that enumerates a specific list of choices as part of
 * the application. To do this, you just create a
 * <code>DwtMessageComposite</code> and set the message format, like so:
 * <pre>
 * var comp = new DwtMessageComposite(parent);
 * comp.setFormat(MyMsg.repeatTimes);
 * </pre>
 * <p>
 * The message composite instantiates an <code>AjxMessageFormat</code>
 * from the specified message pattern. Then, for each segment it creates
 * static text or a <code>DwtInputField</code> for replacement segments
 * such as "{0}".
 * <p>
 * To have more control over the controls that are created and inserted
 * into the resulting composite, you can pass a callback object to the
 * method. Each time that a replacement segment is found in the
 * message pattern, the callback is called with the following parameters:
 * <ul>
 * <li>a reference to this message composite object;
 * <li>a reference to the segment object.
 * <li>the index at which the segment was found in the message pattern; and
 * </ul>
 * The segment object will be an instance of
 * <code>AjxMessageFormat.MessageSegment</code> and has the following
 * methods of interest:
 * <ul>
 * <li>toSubPattern
 * <li>getIndex
 * <li>getType
 * <li>getStyle
 * <li>getSegmentFormat
 * </ul>
 * <p>
 * The callback can use this information to determine whether or not
 * a custom control should be created for the segment. If the callback
 * returns <code>null</code>, a standard <code>DwtInputField</code> is
 * created and inserted. Note: if the callback returns a custom control,
 * it <em>must</em> be an instance of <code>AjxControl</code>.
 * <p>
 * Here is an example of a message composite created with a callback
 * that generates a custom control for each replacement segment:
 * <pre>
 * function createCustomControl(parent, segment, i) {
 *     return new DwtInputField(parent);
 * }
 *
 * var compParent = ...;
 * var comp = new DwtMessageComposite(compParent);
 *
 * var message = MyMsg.repeatTimes;
 * var callback = new AjxCallback(null, createCustomControl);
 * comp.setFormat(message, callback);
 * </pre>
 *
 * @constructor
 * @class
 *
 * @author Andy Clark
 *
 * @param parent    [DwtComposite]  The parent widget.
 * @param className [string]    CSS class.
 * @param posStyle  [number]    Position style.
 */
function DwtMessageComposite(parent, className, posStyle) {
	if (arguments.length == 0) return;
	className = className || "DwtMessageComposite";
	DwtComposite.call(this, parent, className, posStyle);
}

DwtMessageComposite.prototype = new DwtComposite;
DwtMessageComposite.prototype.constructor = DwtMessageComposite;

DwtMessageComposite.prototype.toString =
function() {
	return "DwtMessageComposite";
}

// Data

DwtMessageComposite.prototype._formatter;
DwtMessageComposite.prototype._controls;

// Public methods

/**
 * @param message   [string]    The message that defines the text and
 *                              controls that comprise this composite.
 * @param callback  [AjxCallback]   (Optional) Callback to create UI
 *                                  components.
 */
DwtMessageComposite.prototype.setFormat =
function(message, callback) {
    // create formatter
    this._formatter = new AjxMessageFormat(message);
    this._controls = {};

    // create HTML
    var id = Dwt.getNextId();
    var a = ["<table border='0'><tr valign='center'>"];

    var segments = this._formatter.getSegments();
    var cells = {};
    for (var i = 0; i < segments.length; i++) {
        var cid = [id,i].join("_");
        a.push("<td id='",cid,"'>");

        var segment = segments[i];
        if (segment instanceof AjxMessageFormat.MessageSegment) {
            var control = callback ? callback.run(this, segment, i) : null;
            if (!control) {
                control = new DwtInputField({parent:this});
            }
            cells[cid] = control.getHtmlElement();

            var sindex = segment.getIndex();
            this._controls[sindex] = this._controls[sindex] || control;
        }
        else {
            a.push(segment.toSubPattern());
        }

        a.push("</td>");
    }

    a.push("</tr></table>");

    // insert HTML
    var el = this.getHtmlElement();
    el.innerHTML = a.join("");

    // insert controls
    for (var cid in cells) {
        var cell = cells[cid];
        var parentEl = document.getElementById(cid);
        parentEl.appendChild(cell);
    }
};

DwtMessageComposite.prototype.format = function() {
    var args = [];
    for (var sindex in this._controls) {
        args[sindex] = this._controls[sindex].getValue();
    }
    return this._formatter.format(args);
};
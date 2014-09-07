Nuxeo Labs
===================

Nuxeo Solution Architects team produces prototypes for our current and future customers to help them solve a specific problem they have that is not currently a part of the Nuxeo Platform. When something that could be valuable to other users comes out of it, we publish them openly into the nuxeo-labs repository.

### Build

Assuming *maven* (3.2.1) is installed on your system, after downloading the whole repository, just:

```
cd /path/to/nuxeo-labs
mvn install
```

The MP is in `nuxeo-labs-mp/target`, named `nuxeo-labs-mp-{version}.zip`. it can be installed from the Admin. Center (see [this doc](http://doc.nuxeo.com/display/ADMINDOC/Installing+a+new+package+on+your+instance#InstallingaNewPackageonYourInstance-OfflineInstallation), of from the commandline (using `nuxeoctl mp-install`).

We plan to make it available from the "Package from Nuxeo Marketplace" tab. But it is not done yet so far.


### Please, Notice That...

**These features are not a part of the supported Production platform** Nuxeo does not support usage of this code as part of any Nuxeo Connect Subscription. This is a moving project (no API maintenance, no depreciation process, etc...). If some developments can be useful for the Nuxeo Platform according Tech Lead team, we will move these develoments into the next version of Nuxeo Platform. Note: this has alredy happened and Nuxeo Labs contains good candidates for integration in the platform.

Feel free to use it or be inspired for your own code.

### What's Inside?

* [_nuxeo-labs-fancybox_](https://github.com/nuxeo/nuxeo-labs/tree/master/nuxeo-labs-fancybox): In Studio, define a layout, a callback Automation Chain and an XML Extension. You now have a Fancybox dialog, and in the automation chain, you get the values enetered by the user
* [_nuxeo-labs-images_](https://github.com/nuxeo/nuxeo-labs/tree/master/nuxeo-labs-images):
  * Operations to convert, watermark, crop, ... images
  * Crop toolbar button with UI
* [_nuxeo-labs-operations_](https://github.com/nuxeo/nuxeo-labs/tree/master/nuxeo-labs-operations): Contains interesting operations about list management and an advanced email operation with an easier signature and cc/bcc/replyto configuration
* [_nuxeo-labs-signature_](https://github.com/nuxeo/nuxeo-labs/tree/master/nuxeo-labs-signature) provides an automation operation which applies a digital signature to an input pdf file
* [_nuxeo-labs-template-rendition-publisher_](https://github.com/nuxeo/nuxeo-labs/tree/master/nuxeo-labs-template-rendition-publisher): An Automation operation that enables to publish a template rendition. You can use this operation with the `nuxeo-template-rendering` plug-in
* [_nuxeo-labs-video_](https://github.com/nuxeo/nuxeo-labs/tree/master/nuxeo-labs-video):
  * Helper class (using `MediaInfoHelper`) to call `mediainfo -i` on video blobs and parse the result.
  * With operations you can use in your chains
* [_nuxeo-labs-we-publication_](https://github.com/nuxeo/nuxeo-labs/tree/master/nuxeo-labs-we-publication): Displays the `Sections` as a cool webengine application
* Others:
  * [_nuxeo-labs-dam-default-tab_](https://github.com/nuxeo/nuxeo-labs/tree/master/nuxeo-labs-dam-default-tab)
    *  Overrides the default startup helper to select the DAM view by default when a user connects to the application
    *  *NOTE*: It is not included in the Marketplace Package and its .jar must be installed manually (typically in the `plugins` folder of `nxserver`)
  * [_resources_](https://github.com/nuxeo/nuxeo-labs/tree/master/resources) are free to use resources for your Studio project (icons, background image, etc...)

### About Nuxeo

Nuxeo provides a modular, extensible Java-based [open source software platform for enterprise content management](http://www.nuxeo.com/en/products/ep) and packaged applications for [document management](http://www.nuxeo.com/en/products/document-management), [digital asset management](http://www.nuxeo.com/en/products/dam) and [case management](http://www.nuxeo.com/en/products/case-management). Designed by developers for developers, the Nuxeo platform offers a modern architecture, a powerful plug-in model and extensive packaging capabilities for building content applications.

More information on: <http://www.nuxeo.com/>

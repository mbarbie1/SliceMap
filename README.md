# SliceMap

This is a FIJI plugin for automated brain region annotation of fluorescent brain slices. The plugin uses a reference library of pre-annotated brain slices (the brain region templates) to annotate brain regions of unknown samples. To perform the region annotation it will register the reference slices to the sample slice (using elastic registration plugin BunwarpJ) and use the resulting image transformations  to morph the template regions towards the anatomic brain regions of the sample. A more detailed explaination can be found in the paper (under review).

An explaination of the usage and input/output of the SliceMap plugin is given in the "user_manual.docx" file.

## Getting started

### Prerequisites

Minimal is to have a java virtual machine installed on your computer (nowadays often installed by default). SliceMap can then be used as a standalone. But it is much more convenient to have FIJI (ImageJ) installed (also necessary to use the ouput regions which are ImageJ ROI's to further process the images).

### Installation

To use SliceMap one can either copy the jar-file SliceMap_-1.0-SNAPSHOT-jar-with-dependencies.jar into the plugins folder of the ImageJ/FIJI application (typically this folder is …/Fiji.app/plugins/), or just start it (by e.g. double clicking it), this way it will open its own ImageJ instance to run in.

### Running SliceMap on the example dataset

The example dataset can be found in the folder "example". How to run it is explained in the "user_manual.docx" file.

## Authors

Michael Barbier and Winnok De Vos

## License

This project is licensed under the MIT License - see the LICENSE.md file for details

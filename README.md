# TWM-LL-PC-Controller
This project contains program for reading out signals and controlling temperature. It's all about apparatus for measuring thermal diffusivity using Temperature Wave Method. (pictures coming later).

Is is tightly coupled with ICP DAS PCI-1602 board. It's still work in progress but should contain the following modules:
* **AI/AOLL**. ADC/DAC Low-level module. That has to confgure ADC in a particular mode, poll data from ADC and organize cyclic buffers (channels) for other parts to use.
* **TCntr** Temerature controller that recieves data from ADC part of **AI/AOLL** module, computes signals for temperature manipulation and outputs it to DAC part.
* **PhCo** Phase computer. Module that reads data from **AI/AOLL** and computes phase shift between multiple channels.
* **PhyMod** Physics model. Talks to **phase computer** and configuration files (or GUI) to get some constants for computing thermal diffusivity.
* **ModCntr** Modulator controller. Module to control the frequency of laser heat wave.
* **GUI** Some sort of GUI for configuring constants and showing graphs (some onine data, like digital oscilloscope, and measurement results)

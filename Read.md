The goal of this assignment is to evaluate your proficiency with Flutter architectural design, native platform integration (Swift/Kotlin), and low-level Bluetooth Low Energy (BLE) protocols.

The Challenge: BLE Clone Application
You are required to build a cross-platform Flutter application that can discover nearby BLE devices, read their profile structures, and then "clone" those characteristics by turning the mobile device itself into a BLE Peripheral hosting those exact specifications.

Core Requirements
The Flutter UI/Architecture:

A clean interface to scan for and list nearby BLE Central devices.

A detailed view displaying a selected device's Services and Characteristics (UUIDs, Properties, and Permissions).

A "Clone" action button that stops scanning and immediately initializes the device as a BLE Peripheral broadcasting the discovered profile.

The Native Layer Requirement (Strict):

No pre-made Flutter BLE plugins (such as flutter_blue_plus) are allowed for the core logic.

All BLE operations—including scanning, GATT discovery, peripheral management, and advertising—must be written natively using Swift (CoreBluetooth) or Kotlin (BluetoothGatt).

Communication between Flutter and the native layer must be handled cleanly via MethodChannels and/or EventChannels.

Platform Choice:

You may choose to implement the native layer for either iOS or Android—whichever showcases your strongest platform-specific expertise. Implementing both is a bonus but is not required.

Timeline & AI Tool Policy
Deadline: Please submit your completed assessment by June 8, 2026.

AI Tool Policy: The use of AI assistants (such as ChatGPT, Claude, or GitHub Copilot) is fully permitted and encouraged for boilerplate generation, debugging, or optimization. We believe in leveraging modern tools to maximize efficiency. However, please ensure you thoroughly understand your code, as you will be asked to explain your architectural choices and native implementation details in the subsequent technical interview.

Technical Expectations & Evaluation Criteria
Architecture: Clear separation of concerns between your Flutter presentation layer and the native MethodChannel implementation.

BLE Lifecycle Management: Proper handling of Bluetooth adapter states (e.g., handling Bluetooth turned off, location permissions missing, or hardware limitations).

Code Quality: Well-commented, readable Swift/Kotlin and Dart code following platform idiomatic best practices.

Submission Guidelines
Repository: Please upload your project to a public GitHub/GitLab repository and share the link with us.

Documentation: Include a brief README.md explaining:

How to set up and run the project.

Your architectural choices regarding the MethodChannel data mapping.

Any platform-specific limitations you encountered (e.g., background limitations, characteristic value caching constraints).
import tensorflow as tf
import onnxruntime

class InterpreterTFLite:
    def __init__(self, model_path, output_index=0):
        self.model_path = model_path
        self.output_index = output_index

        self.interpreter = tf.lite.Interpreter(model_path=self.model_path)

    def predict(self, data):
        self.interpreter.allocate_tensors()

        # Get input and output tensors.
        input_details = self.interpreter.get_input_details()
        output_details = self.interpreter.get_output_details()

        self.interpreter.set_tensor(input_details[0]['index'], data)
        self.interpreter.invoke()

        # The function `get_tensor()` returns a copy of the tensor data.
        # Use `tensor()` in order to get a pointer to the tensor.
        output_data = self.interpreter.get_tensor(output_details[self.output_index]['index'])
        return output_data

class InterpreterTF:
    def __init__(self, weights_path, type="yawn"):
        self.model = self.create_model(type)
        self.model.load_weights(weights_path)

    def create_model(self, type):
        if type == "yawn":
            img_shape = (512, 512, 3)
        elif type == "eye":
            img_shape = (128, 128, 3)
        else:
            raise Exception("Invalid type")

        base_model = tf.keras.applications.MobileNetV2(
            input_shape=img_shape,
            include_top=False,
            weights='imagenet'
        )
        base_model.trainable = False

        if type == "yawn":
            return tf.keras.Sequential([
                base_model,
                tf.keras.layers.Conv2D(32, 3, activation='relu'),
                tf.keras.layers.MaxPooling2D(pool_size=(2, 2)),
                tf.keras.layers.Dropout(0.2),
                
                tf.keras.layers.Conv2D(64, 3, activation='relu'),
                tf.keras.layers.MaxPooling2D(pool_size=(2, 2)),
                tf.keras.layers.Dropout(0.2),

                tf.keras.layers.GlobalAveragePooling2D(),
                tf.keras.layers.Dense(1, activation='sigmoid')
            ])
        else:
            return tf.keras.Sequential([
                base_model,
                tf.keras.layers.Conv2D(32, 3, activation='relu'),
                tf.keras.layers.Dropout(0.2),
                
                tf.keras.layers.GlobalAveragePooling2D(),
                tf.keras.layers.Dense(1, activation='sigmoid')
            ])

    def predict(self, data):
        output = self.model.predict(data, verbose=0).squeeze()
        return (output > 0.5).astype(int)

class InterpreterOnnx:
    def __init__(self, model_path):
        self.session = onnxruntime.InferenceSession(model_path, None)
        self.input_name = self.session.get_inputs()[0].name

    def predict(self, data):
        output = self.session.run([], {self.input_name: data})
        return output[0]


import tensorflow as tf

class Interpreter:
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
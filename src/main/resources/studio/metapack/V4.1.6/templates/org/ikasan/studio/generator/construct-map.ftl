            if (!(body instanceof java.util.Map)) throw new TransformationException("Expected JMS MapMessage content");
            return (java.util.Map) body;

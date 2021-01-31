import React from 'react'
import { render } from 'react-dom'

import { DesignCanvas, UrlViewer, ShapedGeometry } from 'react-vzome-viewer'

import dodec from './models/dodecahedron/converted'
// import logo from './models/logo'

// const convertLegacyFormat = rawDesign => ({
//   ...rawDesign,
//   instances: rawDesign.instances.map( instance => ({
//     ...instance,
//     shapeId: instance.shape,
//     position: Object.values( instance.position ),
//     rotation: instance.rotation && Object.values( instance.rotation ),
//   }))
// })

const viewerStyle = {
  height: "400px",
  minHeight: "400px",
  maxHeight: "60vh",
  marginLeft: "15%",
  marginRight: "15%",
  marginTop: "15px",
  marginBottom: "15px",
  borderWidth: "medium",
  borderRadius: "10px",
  border: "solid",
}

export const Demo = () =>
{
  return (
    <div>
      <div style={viewerStyle}>
        <DesignCanvas>
          <ShapedGeometry {...dodec} />
        </DesignCanvas>
      </div>
      <div style={viewerStyle}>
        <UrlViewer url={"/models/C240.vZome"} />
      </div>
    </div>
  )
}

render(<Demo/>, document.querySelector('#root'))

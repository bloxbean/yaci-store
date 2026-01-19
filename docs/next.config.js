import nextra from 'nextra'

const withNextra = nextra({
  latex: true,
  search: {
    codeblocks: false
  }
})

export default withNextra({
    images: {
        unoptimized: true,
    },
    // output: 'export'  // Temporarily disabled for development
})
